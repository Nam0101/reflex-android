package androidshowcase.reflex.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient

const val POKE_API_BASE_URL = "https://pokeapi.co/api/v2/"

private const val REQUEST_TIMEOUT_MS = 30_000L
private const val CONNECT_TIMEOUT_MS = 15_000L
private const val HTTP_CACHE_BYTES = 20L * 1024 * 1024

/** Production engine. The CLI and behavior tests pass a fixture engine instead. */
fun defaultHttpEngine(): HttpClientEngine = OkHttp.create()

/**
 * The OkHttp client of a platform that keeps one HTTP stack. With a [cacheDir], responses are stored on disk
 * for as long as their `Cache-Control` allows (PokeAPI: one day), so reopening a screen costs no network.
 * An image loader shares its connection pool and threads through `newBuilder()`.
 */
fun createOkHttpClient(cacheDir: File? = null): OkHttpClient = OkHttpClient.Builder()
  .apply { if (cacheDir != null) cache(Cache(cacheDir, HTTP_CACHE_BYTES)) }
  .build()

/** An engine over a client the platform owns: closing the engine leaves [client] open. */
fun okHttpEngine(client: OkHttpClient): HttpClientEngine = OkHttp.create { preconfigured = client }

fun createHttpClient(
  engine: HttpClientEngine,
  baseUrl: String = POKE_API_BASE_URL,
  logger: ((String) -> Unit)? = null,
): HttpClient = HttpClient(engine) {
  expectSuccess = true
  defaultRequest { url(baseUrl) }
  install(ContentNegotiation) {
    json(Json { ignoreUnknownKeys = true })
  }
  install(HttpTimeout) {
    requestTimeoutMillis = REQUEST_TIMEOUT_MS
    connectTimeoutMillis = CONNECT_TIMEOUT_MS
  }
  if (logger != null) {
    install(Logging) {
      this.logger = object : Logger {
        override fun log(message: String) = logger(message)
      }
      level = LogLevel.INFO
    }
  }
}
