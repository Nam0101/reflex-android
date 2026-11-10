package androidshowcase.reflex.cli

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.isSuccess
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration
import kotlinx.coroutines.delay

/**
 * The network edge of a headless session. Every request is answered from `resources/fixtures` or
 * recorded as a violation: a behavior test fails if any request had no fixture, even when the app
 * recovered from the error. Nothing ever leaves the process, unless [recordTo] is set.
 *
 * A fixture is a file named after its request, so a new endpoint needs a file and no code:
 * - `GET https://pokeapi.co/api/v2/pokemon/25` → `fixtures/pokeapi.co/api/v2/pokemon/25.json`
 * - a query is appended, sorted by name: `…/pokemon?offset=0&limit=20` → `…/pokemon@limit=20,offset=0.json`
 * - other methods add their name: `POST …/favorites` → `…/favorites.post.json`
 *
 * [failOnce] and [delay] take the end of that name, from a `/`: `pokemon/25`, `pokemon@limit=20,offset=0`.
 *
 * With [recordTo] (`reflex headless --record <dir>`), a request without a fixture goes to the real server and
 * its response is saved under that name in the directory. Trim a recorded file to the fields the DTO reads.
 */
class FixtureHttp(private val recordTo: Path? = null) : AutoCloseable {
  private val failures = ConcurrentHashMap<String, ConcurrentLinkedQueue<HttpStatusCode>>()
  private val delays = ConcurrentHashMap<String, Duration>()
  private val network = lazy { HttpClient(OkHttp) }

  val violations: MutableList<String> = CopyOnWriteArrayList()

  /**
   * Answers the next request for the fixture [name] with [status]; the requests after it get the fixture again.
   * A test declares its whole scenario this way before the journey starts: fixtures are never patched mid-test.
   */
  fun failOnce(name: String, status: HttpStatusCode) {
    failures.getOrPut(name, ::ConcurrentLinkedQueue).add(status)
  }

  /** Every request for the fixture [name] takes [duration]: a slow server, to observe loading states. */
  fun delay(name: String, duration: Duration) {
    delays[name] = duration
  }

  val engine: HttpClientEngine = MockEngine { request ->
    val name = fixtureName(request)
    delays.entries.firstOrNull { name.endsWithName(it.key) }?.let { delay(it.value) }
    val failure = failures.entries.firstOrNull { name.endsWithName(it.key) }?.value?.poll()
    val body = if (failure == null) read(name) ?: record(name, request) else null
    when {
      failure != null -> respondError(failure)

      body != null -> respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

      else -> {
        violations += "${request.method.value} ${request.url} (no fixtures/$name.json)"
        error("No fixture for ${request.method.value} ${request.url}")
      }
    }
  }

  override fun close() {
    if (network.isInitialized()) network.value.close()
  }

  private fun read(name: String): ByteArray? {
    val recorded = recordTo?.resolve("$name.json")?.takeIf(Files::exists)
    return recorded?.let(Files::readAllBytes)
      ?: javaClass.getResourceAsStream("/fixtures/$name.json")?.use { it.readBytes() }
  }

  /** Null when not recording, or when the real server did not answer with a 2xx (then it is a violation). */
  private suspend fun record(name: String, request: HttpRequestData): ByteArray? {
    val file = recordTo?.resolve("$name.json") ?: return null
    val response = network.value.request(request.url) {
      method = request.method
      headers { appendAll(request.headers) }
      setBody(request.body)
    }
    if (!response.status.isSuccess()) return null
    val bytes = response.bodyAsBytes()
    Files.createDirectories(file.parent)
    Files.write(file, bytes)
    System.err.println("Recorded ${request.method.value} ${request.url} → $file")
    return bytes
  }

  private fun fixtureName(request: HttpRequestData): String {
    val url = request.url
    val query = url.parameters.entries()
      .flatMap { (key, values) -> values.map { "$key=$it" } }
      .sorted()
      .joinToString(",") { it.replace(UNSAFE_IN_FILE_NAME, "_") }
    return buildString {
      append(url.host).append(url.encodedPath.trimEnd('/'))
      if (query.isNotEmpty()) append('@').append(query)
      if (request.method != HttpMethod.Get) append('.').append(request.method.value.lowercase())
    }
  }

  private fun String.endsWithName(name: String) = this == name || endsWith("/$name")

  private companion object {
    val UNSAFE_IN_FILE_NAME = Regex("[^A-Za-z0-9._=-]")
  }
}
