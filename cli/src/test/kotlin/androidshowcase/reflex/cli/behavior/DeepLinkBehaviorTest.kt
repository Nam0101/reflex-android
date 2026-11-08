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
  fun `a link opened on Home replaces whatever is on top`() {
    onboard()
    app.act("open", """{"id":1}""", expect = "PokemonDetail")
    app.act("openDeepLink", """{"uri":"reflex://pokemon/25"}""", expect = "PokemonDetail", state = """{"id":25}""")
    app.act("goBack", expect = "Pokedex")
  }
}
