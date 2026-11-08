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
import kotlinx.serialization.json.Json

const val POKE_API_BASE_URL = "https://pokeapi.co/api/v2/"

private const val REQUEST_TIMEOUT_MS = 30_000L
private const val CONNECT_TIMEOUT_MS = 15_000L

/** Production engine. The CLI and behavior tests pass a fixture engine instead. */
fun defaultHttpEngine(): HttpClientEngine = OkHttp.create()

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
