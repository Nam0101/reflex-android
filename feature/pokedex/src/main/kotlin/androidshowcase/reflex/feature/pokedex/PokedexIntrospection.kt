package androidshowcase.reflex.feature.pokedex

import androidshowcase.reflex.introspection.ViewModelIntrospectionNode
import androidshowcase.reflex.introspection.action

class PokedexViewModelIntrospectionNode(viewModel: PokedexViewModel) :
  ViewModelIntrospectionNode<PokedexViewModel.State, PokedexViewModel.Action>(
    viewModel,
    PokedexViewModel.State.serializer(),
  ) {
  override val name = "Pokedex"

  override val actions = listOf(
    action("loadMore", "Scroll to the end") { send(PokedexViewModel.Action.LoadMore) },
    action("retry", "Retry") { send(PokedexViewModel.Action.Retry) },
    action("open", "Open a Pokémon", PokedexViewModel.Action.Open.serializer()) { send(it) },
  )
}

class PokemonDetailViewModelIntrospectionNode(viewModel: PokemonDetailViewModel) :
  ViewModelIntrospectionNode<PokemonDetailViewModel.State, PokemonDetailViewModel.Action>(
    viewModel,
    PokemonDetailViewModel.State.serializer(),
  ) {
  override val name = "PokemonDetail"

  override val actions = listOf(
    action("retry", "Retry") { send(PokemonDetailViewModel.Action.Retry) },
    action("toggleFavorite", "Toggle favorite") { send(PokemonDetailViewModel.Action.ToggleFavorite) },
  )
}

class FavoritesViewModelIntrospectionNode(viewModel: FavoritesViewModel) :
  ViewModelIntrospectionNode<FavoritesViewModel.State, FavoritesViewModel.Action>(
    viewModel,
    FavoritesViewModel.State.serializer(),
  ) {
  override val name = "Favorites"

  override val actions = listOf(
    action("open", "Open a Pokémon", FavoritesViewModel.Action.Open.serializer()) { send(it) },
    action("remove", "Remove from favorites", FavoritesViewModel.Action.Remove.serializer()) { send(it) },
  )
}
