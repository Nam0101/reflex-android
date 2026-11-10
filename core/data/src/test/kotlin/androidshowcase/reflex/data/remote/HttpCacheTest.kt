package androidshowcase.reflex.data.remote

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HttpCacheTest {
  private val hits = AtomicInteger()
  private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
    createContext("/pokemon/25") { exchange ->
      hits.incrementAndGet()
      val body = """{"id":25,"name":"pikachu","height":4,"weight":60,"types":[],"stats":[]}""".toByteArray()
      exchange.responseHeaders.add("Content-Type", "application/json")
      exchange.responseHeaders.add("Cache-Control", "public, max-age=86400")
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.use { it.write(body) }
    }
    start()
  }

  @AfterEach
  fun shutdown() = server.stop(0)

  @Test
  fun `a cacheable response is fetched once with a cache directory, every time without`(@TempDir cacheDir: File) {
    assertEquals(2, hitsForTwoRequests(cacheDir = null))
    assertEquals(1, hitsForTwoRequests(cacheDir))
  }

  private fun hitsForTwoRequests(cacheDir: File?): Int = runBlocking {
    hits.set(0)
    val okHttp = createOkHttpClient(cacheDir)
    createHttpClient(okHttpEngine(okHttp), baseUrl = "http://127.0.0.1:${server.address.port}/").use { client ->
      repeat(2) { assertEquals("pikachu", client.pokemon(25).name) }
    }
    okHttp.dispatcher.executorService.shutdown()
    okHttp.connectionPool.evictAll()
    hits.get()
  }
}
