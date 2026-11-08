package androidshowcase.reflex.feature.pokedex

import androidshowcase.reflex.navigation.UiScope
import androidshowcase.reflex.navigation.screen
import org.koin.core.module.Module
import org.koin.dsl.module

/** The pokedex screens. The shell loads this module and implements [PokedexNavigation]. */
fun pokedexModule(): Module = module {
  screen<PokedexRoute> { PokedexViewModel(get(), get(), get(UiScope)) }
  screen<FavoritesRoute> { FavoritesViewModel(get(), get(), get(UiScope)) }
  screen<PokemonDetailRoute> { route -> PokemonDetailViewModel(route.id, get(), get(), get(UiScope)) }
}
