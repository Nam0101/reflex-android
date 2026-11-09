package androidshowcase.reflex.cli

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The network edge of a headless session. Every request is answered from `resources/fixtures` or
 * recorded as a violation: a behavior test fails if any request had no fixture, even when the app
 * recovered from the error. Nothing ever leaves the process.
 *
 * Fixture names: `pokemon-list-<offset>` for `GET pokemon?offset=…&limit=20`, `pokemon-<id>` for `GET pokemon/<id>`.
 */
class FixtureHttp {
  private val failures = ConcurrentHashMap<String, ConcurrentLinkedQueue<HttpStatusCode>>()

  val violations: MutableList<String> = CopyOnWriteArrayList()

  /**
   * Answers the next request for the fixture [name] with [status]; the requests after it get the fixture again.
   * A test declares its whole scenario this way before the journey starts: fixtures are never patched mid-test.
   */
  fun failOnce(name: String, status: HttpStatusCode) {
    failures.getOrPut(name, ::ConcurrentLinkedQueue).add(status)
  }

  val engine: HttpClientEngine = MockEngine { request ->
    val name = fixtureName(request)
    val failure = name?.let { failures[it]?.poll() }
    val body = name?.let {
      javaClass.getResourceAsStream("/fixtures/$it.json")?.use { stream -> stream.readBytes().decodeToString() }
    }
    when {
      failure != null -> respondError(failure)

      body != null -> respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

      else -> {
        violations += "${request.method.value} ${request.url}"
        error("No fixture for ${request.method.value} ${request.url}")
      }
    }
  }

  private fun fixtureName(request: HttpRequestData): String? {
    if (request.url.host != "pokeapi.co") return null
    val path = request.url.encodedPath.removePrefix("/api/v2/").trimEnd('/')
    return when {
      path == "pokemon" && request.url.parameters["limit"] == "20" -> request.url.parameters["offset"]?.let {
        "pokemon-list-$it"
      }

      path.startsWith("pokemon/") -> "pokemon-${path.removePrefix("pokemon/")}"

      else -> null
    }
  }
}
