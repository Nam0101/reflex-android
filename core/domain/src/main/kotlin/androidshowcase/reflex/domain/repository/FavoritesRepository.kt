package androidshowcase.reflex.domain.repository

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.PokemonSummary
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
  /** Newest first. Emits again on every change. */
  fun favorites(): Flow<Result<List<PokemonSummary>, AppError>>

  fun isFavorite(id: Int): Flow<Result<Boolean, AppError>>

  suspend fun add(pokemon: PokemonSummary): Result<Unit, AppError>

  suspend fun remove(id: Int): Result<Unit, AppError>
}
