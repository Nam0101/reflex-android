package androidshowcase.reflex.cli.behavior

import org.junit.jupiter.api.Test

class DeepLinkBehaviorTest : BehaviorTest() {
  @Test
  fun `a link opened during onboarding waits for Home, then opens over it`() {
    app.precondition("Language")
    app.act("openDeepLink", """{"uri":"reflex://pokemon/25"}""", state = """{"selected":null}""")
    app.refused("openDeepLink", """{"uri":"https://example.com/pokemon/25"}""")
    app.act("select", """{"language":"English"}""")
    app.act("continue", expect = "Onboarding")
    app.act("finish", expect = "PokemonDetail", state = """{"id":25,"isLoading":false}""")
    app.act("goBack", expect = "Pokedex")
  }

  @Test
  fun `a link opened on Home replaces whatever is on top and keeps Home as it was`() {
    onboard()
    app.act("loadMore", state = """{"endReached":true}""")
    app.act("open", """{"id":1}""", expect = "PokemonDetail")
    app.act("openDeepLink", """{"uri":"reflex://pokemon/25"}""", expect = "PokemonDetail", state = """{"id":25}""")
    // Home was not rebuilt: the list still holds both pages, nothing is fetched again.
    app.act("goBack", expect = "Pokedex", state = """{"endReached":true}""")
  }

  @Test
  fun `a killed process comes back to the screens and tab it had`() {
    onboard()
    app.act("selectTab", """{"tab":"Favorites"}""", expect = "Favorites")
    app.act("openDeepLink", """{"uri":"reflex://pokemon/25"}""", expect = "PokemonDetail", state = """{"id":25}""")
    val saved = checkNotNull(session.app.saveNavigation())
    app.act("goBack", expect = "Favorites")

    // The same process ignores its own snapshot (an Activity recreated by a rotation): nothing reopens.
    session.app.restoreNavigation(saved)
    app.precondition("Favorites")

    session.relaunch()
    session.app.restoreNavigation("not a snapshot")
    session.app.restoreNavigation(saved)
    app.precondition("PokemonDetail", """{"id":25,"isLoading":false}""")
    app.act("goBack", expect = "Favorites")
  }
}
