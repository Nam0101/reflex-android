package androidshowcase.reflex.data.remote

import androidshowcase.reflex.data.catchingRemote
import androidshowcase.reflex.domain.error.AppError
import com.github.michaelbull.result.getError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PokeApiTest {
  @Test
  fun `id is parsed from the resource url, with or without trailing slash`() {
    assertEquals(25, NamedResourceDto("pikachu", "https://pokeapi.co/api/v2/pokemon/25/").pokemonId())
    assertEquals(7, NamedResourceDto("squirtle", "https://pokeapi.co/api/v2/pokemon/7").pokemonId())
  }

  @Test
  fun `next offset advances by the page size and is null on the last page`() {
    val items = listOf(NamedResourceDto("a", "https://x/pokemon/21/"), NamedResourceDto("b", "https://x/pokemon/22/"))
    assertEquals(22, PokemonListDto(next = "https://x?offset=22", results = items).toPage(offset = 20).nextOffset)
    assertNull(PokemonListDto(next = null, results = items).toPage(offset = 20).nextOffset)
  }

  @Test
  fun `http and io failures map to typed errors`() = runTest {
    assertInstanceOf(AppError.NotFound::class.java, failure(HttpStatusCode.NotFound))
    assertEquals(503, (failure(HttpStatusCode.ServiceUnavailable) as AppError.Server).statusCode)
    val offline = createHttpClient(MockEngine { throw UnknownHostException("pokeapi.co") })
    assertInstanceOf(AppError.Network::class.java, catchingRemote { offline.pokemon(1) }.getError())
  }

  @Test
  fun `detail json maps to the domain model`() = runTest {
    val client = createHttpClient(
      MockEngine {
        respond(
          """{"id":25,"name":"pikachu","height":4,"weight":60,
             "types":[{"slot":1,"type":{"name":"electric","url":"u"}}],
             "stats":[{"base_stat":35,"stat":{"name":"hp","url":"u"}}]}""",
          headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
      },
    )
    val detail = client.pokemon(25).toDomain()
    assertEquals(listOf("electric"), detail.types)
    assertEquals(35, detail.stats.single().value)
  }

  private suspend fun failure(status: HttpStatusCode): AppError? {
    val client = createHttpClient(MockEngine { respondError(status) })
    return catchingRemote { client.pokemon(1) }.getError()
  }
}
