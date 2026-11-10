package androidshowcase.reflex.feature.pokedex

import androidshowcase.reflex.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data object PokedexRoute : Route

@Serializable
data object FavoritesRoute : Route

@Serializable
data class PokemonDetailRoute(val id: Int) : Route

fun interface PokedexNavigation {
  fun openPokemon(id: Int)
}
