package androidshowcase.reflex.feature.pokedex

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.Page
import androidshowcase.reflex.domain.model.PokemonSummary
import androidshowcase.reflex.domain.repository.PokemonRepository
import androidshowcase.reflex.viewmodel.Next
import androidshowcase.reflex.viewmodel.ReflexViewModel
import androidshowcase.reflex.viewmodel.next
import androidshowcase.reflex.viewmodel.task
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

class PokedexViewModel(
  private val pokemon: PokemonRepository,
  private val navigation: PokedexNavigation,
  scope: CoroutineScope,
) : ReflexViewModel<PokedexViewModel.State, PokedexViewModel.Action, PokedexViewModel.Message>(State(), scope) {
  @Serializable
  data class State(
    val items: List<PokemonSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
    /** Offset of the next page; null once the last page is loaded. */
    val nextOffset: Int? = 0,
  ) {
    val endReached = nextOffset == null
  }

  sealed interface Action {
    /** The list scrolled near its end. Ignored while loading, after the end, or while an error shows. */
    data object LoadMore : Action

    data object Retry : Action

    @Serializable
    data class Open(val id: Int) : Action
  }

  sealed interface Message {
    data class ViewAction(val action: Action) : Message

    data class PageLoaded(val offset: Int, val result: Result<Page<PokemonSummary>, AppError>) : Message
  }

  override fun viewAction(action: Action) = Message.ViewAction(action)

  override fun onStart() = listOf(load(offset = 0))

  override fun update(state: State, message: Message): Next<State, Message> = when (message) {
    is Message.ViewAction -> when (val action = message.action) {
      Action.LoadMore -> if (state.error != null) state.next() else loadNext(state)

      Action.Retry -> loadNext(state)

      is Action.Open -> state.next(
        task {
          navigation.openPokemon(action.id)
          null
        },
      )
    }

    is Message.PageLoaded ->
      if (message.offset != state.nextOffset) {
        state.next() // a stale page: never append twice
      } else {
        message.result.fold(
          success = { page ->
            state.copy(items = state.items + page.items, nextOffset = page.nextOffset, isLoading = false).next()
          },
          failure = { state.copy(isLoading = false, error = it).next() },
        )
      }
  }

  private fun loadNext(state: State): Next<State, Message> {
    val offset = state.nextOffset
    return if (state.isLoading || offset == null) {
      state.next()
    } else {
      state.copy(isLoading = true, error = null).next(load(offset))
    }
  }

  private fun load(offset: Int) = task<Message> { Message.PageLoaded(offset, pokemon.page(offset, PAGE_SIZE)) }

  companion object {
    const val PAGE_SIZE = 20
  }
}
