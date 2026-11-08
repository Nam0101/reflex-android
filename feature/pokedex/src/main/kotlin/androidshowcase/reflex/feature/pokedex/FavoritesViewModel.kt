package androidshowcase.reflex.feature.pokedex

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.PokemonSummary
import androidshowcase.reflex.domain.repository.FavoritesRepository
import androidshowcase.reflex.viewmodel.Next
import androidshowcase.reflex.viewmodel.ReflexViewModel
import androidshowcase.reflex.viewmodel.next
import androidshowcase.reflex.viewmodel.stream
import androidshowcase.reflex.viewmodel.task
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

class FavoritesViewModel(
  private val favorites: FavoritesRepository,
  private val navigation: PokedexNavigation,
  scope: CoroutineScope,
) : ReflexViewModel<FavoritesViewModel.State, FavoritesViewModel.Action, FavoritesViewModel.Message>(State(), scope) {
  @Serializable
  data class State(
    val items: List<PokemonSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
  )

  sealed interface Action {
    @Serializable
    data class Open(val id: Int) : Action

    @Serializable
    data class Remove(val id: Int) : Action
  }

  sealed interface Message {
    data class ViewAction(val action: Action) : Message

    data class Loaded(val result: Result<List<PokemonSummary>, AppError>) : Message

    data class Removed(val result: Result<Unit, AppError>) : Message
  }

  override fun viewAction(action: Action) = Message.ViewAction(action)

  override fun onStart() = listOf(stream<Message>(FAVORITES_STREAM, favorites.favorites().map { Message.Loaded(it) }))

  override fun update(state: State, message: Message): Next<State, Message> = when (message) {
    is Message.ViewAction -> when (val action = message.action) {
      is Action.Open -> state.next(
        task {
          navigation.openPokemon(action.id)
          null
        },
      )

      is Action.Remove -> state.copy(error = null).next(task { Message.Removed(favorites.remove(action.id)) })
    }

    is Message.Loaded -> message.result.fold(
      success = { state.copy(items = it, isLoading = false, error = null).next() },
      failure = { state.copy(isLoading = false, error = it).next() },
    )

    is Message.Removed -> message.result.fold(
      success = { state.next() }, // the database stream delivers the new list
      failure = { state.copy(error = it).next() },
    )
  }

  private companion object {
    const val FAVORITES_STREAM = "favorites"
  }
}
