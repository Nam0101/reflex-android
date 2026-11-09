package androidshowcase.reflex.feature.pokedex

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.PokemonDetail
import androidshowcase.reflex.domain.repository.FavoritesRepository
import androidshowcase.reflex.domain.repository.PokemonRepository
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

class PokemonDetailViewModel(
  private val id: Int,
  private val pokemon: PokemonRepository,
  private val favorites: FavoritesRepository,
  scope: CoroutineScope,
) : ReflexViewModel<PokemonDetailViewModel.State, PokemonDetailViewModel.Action, PokemonDetailViewModel.Message>(
  State(id = id, artworkUrl = pokemon.artworkUrl(id)),
  scope,
) {
  @Serializable
  data class State(
    val id: Int,
    /** Known from the start, so the screen shows the artwork while the detail loads. */
    val artworkUrl: String,
    val pokemon: PokemonDetail? = null,
    val isLoading: Boolean = true,
    /** Loading the Pokémon failed. */
    val error: AppError? = null,
    /** Read from the database: it flips only once the write is stored. */
    val isFavorite: Boolean = false,
    val isSavingFavorite: Boolean = false,
    val favoriteError: AppError? = null,
  )

  sealed interface Action {
    data object Retry : Action

    data object ToggleFavorite : Action
  }

  sealed interface Message {
    data class ViewAction(val action: Action) : Message

    data class Loaded(val result: Result<PokemonDetail, AppError>) : Message

    data class FavoriteChanged(val result: Result<Boolean, AppError>) : Message

    data class FavoriteSaved(val result: Result<Unit, AppError>) : Message
  }

  override fun viewAction(action: Action) = Message.ViewAction(action)

  override fun onStart() = listOf(
    load(),
    stream<Message>(FAVORITE_STREAM, favorites.isFavorite(id).map(Message::FavoriteChanged)),
  )

  override fun update(state: State, message: Message): Next<State, Message> = when (message) {
    is Message.ViewAction -> when (message.action) {
      Action.Retry -> if (state.isLoading) state.next() else state.copy(isLoading = true, error = null).next(load())

      Action.ToggleFavorite -> {
        val loaded = state.pokemon
        if (loaded == null || state.isSavingFavorite) {
          state.next()
        } else {
          val remove = state.isFavorite
          state.copy(isSavingFavorite = true, favoriteError = null).next(
            task { Message.FavoriteSaved(if (remove) favorites.remove(id) else favorites.add(loaded.summary)) },
          )
        }
      }
    }

    is Message.Loaded -> message.result.fold(
      success = { state.copy(pokemon = it, isLoading = false).next() },
      failure = { state.copy(isLoading = false, error = it).next() },
    )

    is Message.FavoriteChanged -> message.result.fold(
      success = { state.copy(isFavorite = it).next() },
      failure = { state.copy(favoriteError = it).next() },
    )

    is Message.FavoriteSaved -> message.result.fold(
      success = { state.copy(isSavingFavorite = false).next() },
      failure = { state.copy(isSavingFavorite = false, favoriteError = it).next() },
    )
  }

  private fun load() = task<Message> { Message.Loaded(pokemon.detail(id)) }

  private companion object {
    const val FAVORITE_STREAM = "favorite"
  }
}
