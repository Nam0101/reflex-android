package androidshowcase.reflex.shell

import androidshowcase.reflex.feature.pokedex.PokemonDetailRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DeepLinksTest {
  @Test
  fun `pokemon links parse, anything else is rejected`() {
    assertEquals(listOf(PokemonDetailRoute(25)), DeepLinks.parse("reflex://pokemon/25"))
    assertEquals(listOf(PokemonDetailRoute(25)), DeepLinks.parse("reflex://pokemon/25/?ref=push"))
    assertNull(DeepLinks.parse("reflex://pokemon/0"))
    assertNull(DeepLinks.parse("reflex://pokemon/abc"))
    assertNull(DeepLinks.parse("reflex://pokemon/25/moves"))
    assertNull(DeepLinks.parse("https://pokemon/25"))
  }
}
