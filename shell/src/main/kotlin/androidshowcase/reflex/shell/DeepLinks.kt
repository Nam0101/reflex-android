package androidshowcase.reflex.shell

import androidshowcase.reflex.feature.pokedex.PokemonDetailRoute
import androidshowcase.reflex.navigation.Route

/** Deep links are navigation state too: a link parses to the routes pushed over Home. */
object DeepLinks {
  const val SCHEME = "reflex"

  /** `reflex://pokemon/25` → `[PokemonDetailRoute(25)]`; null when the link is not ours or is malformed. */
  fun parse(uri: String): List<Route>? {
    val path = uri.trim().removePrefix("$SCHEME://").takeIf { it != uri.trim() } ?: return null
    val segments = path.substringBefore('?').trimEnd('/').split('/')
    return when (segments.firstOrNull()) {
      "pokemon" -> segments.getOrNull(1)
        ?.toIntOrNull()
        ?.takeIf { it > 0 && segments.size == 2 }
        ?.let { listOf(PokemonDetailRoute(it)) }

      else -> null
    }
  }
}
