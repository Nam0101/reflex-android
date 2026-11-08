package androidshowcase.reflex.feature.pokedex

import androidshowcase.reflex.navigation.Route

data object PokedexRoute : Route

data object FavoritesRoute : Route

data class PokemonDetailRoute(val id: Int) : Route

fun interface PokedexNavigation {
  fun openPokemon(id: Int)
}
