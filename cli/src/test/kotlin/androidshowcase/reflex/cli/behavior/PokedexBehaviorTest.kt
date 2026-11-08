package androidshowcase.reflex.cli.behavior

import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

class PokedexBehaviorTest : BehaviorTest() {
  @Test
  fun `browse, open a Pokemon, favorite it and manage it from the Favorites tab`() {
    onboard()
    app.precondition("Pokedex", """{"error":null,"nextOffset":20}""")
    app.act("loadMore", state = """{"isLoading":false,"endReached":true,"nextOffset":null}""")
    app.act("loadMore", state = """{"endReached":true}""")

    app.act("open", """{"id":25}""", expect = "PokemonDetail", state = """{"isLoading":false,"isFavorite":false}""")
    app.precondition("PokemonDetail", """{"pokemon":{"name":"pikachu","types":["electric"],"heightDecimetres":4}}""")
    app.act("toggleFavorite", state = """{"isFavorite":true,"isSavingFavorite":false}""")
    app.act("goBack", expect = "Pokedex")

    app.act(
      "selectTab",
      """{"tab":"Favorites"}""",
      expect = "Favorites",
      state = """{"items":[{"id":25,"name":"pikachu"}]}""",
    )
    app.refused("selectTab", """{"tab":"Settings"}""")
    app.act("open", """{"id":25}""", expect = "PokemonDetail", state = """{"isLoading":false,"isFavorite":true}""")
    app.act("toggleFavorite", state = """{"isFavorite":false}""")
    app.act("goBack", expect = "Favorites", state = """{"items":[]}""")

    app.act("selectTab", """{"tab":"Pokedex"}""", expect = "Pokedex", state = """{"endReached":true}""")
  }

  @Test
  fun `favorites survive a relaunch and can be removed from the list`() {
    onboard()
    app.act("open", """{"id":1}""", expect = "PokemonDetail", state = """{"isLoading":false}""")
    app.act("toggleFavorite", state = """{"isFavorite":true}""")

    session.relaunch()
    app.precondition("Pokedex", """{"isLoading":false}""")
    app.act(
      "selectTab",
      """{"tab":"Favorites"}""",
      expect = "Favorites",
      state = """{"items":[{"name":"bulbasaur"}]}""",
    )
    app.act("remove", """{"id":1}""", state = """{"items":[]}""")
  }

  @Test
  fun `a failed page shows the error, scrolling does not hammer the server, retry recovers`() {
    session.fixtures.fail("pokemon-list-0", HttpStatusCode.InternalServerError)
    app.precondition("Language")
    app.act("select", """{"language":"English"}""")
    app.act("continue", expect = "Onboarding")
    app.act(
      "finish",
      expect = "Pokedex",
      state = """{"isLoading":false,"items":[],"error":{"type":"Server","statusCode":500}}""",
    )
    app.act("loadMore", state = """{"isLoading":false,"error":{"type":"Server"}}""")

    session.fixtures.clearFailures()
    app.act("retry", state = """{"isLoading":false,"error":null,"nextOffset":20}""")
  }

  @Test
  fun `a missing Pokemon shows not found, retry loads it`() {
    onboard()
    session.fixtures.fail("pokemon-25", HttpStatusCode.NotFound)
    app.act(
      "open",
      """{"id":25}""",
      expect = "PokemonDetail",
      state = """{"isLoading":false,"pokemon":null,"error":{"type":"NotFound"}}""",
    )
    app.act("toggleFavorite", state = """{"isFavorite":false,"isSavingFavorite":false}""")

    session.fixtures.clearFailures()
    app.act("retry", state = """{"isLoading":false,"error":null,"pokemon":{"id":25}}""")
  }
}
