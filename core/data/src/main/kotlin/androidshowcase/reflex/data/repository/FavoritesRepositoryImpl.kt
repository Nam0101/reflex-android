package androidshowcase.reflex.data.repository

import androidshowcase.reflex.data.catchingLocal
import androidshowcase.reflex.data.local.FavoriteDao
import androidshowcase.reflex.data.local.FavoriteEntity
import androidshowcase.reflex.data.remote.artworkUrl
import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.PokemonSummary
import androidshowcase.reflex.domain.repository.FavoritesRepository
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class FavoritesRepositoryImpl(private val dao: FavoriteDao, private val now: () -> Long = System::currentTimeMillis) :
  FavoritesRepository {
  override fun favorites(): Flow<Result<List<PokemonSummary>, AppError>> = dao.observeAll()
    .map<List<FavoriteEntity>, Result<List<PokemonSummary>, AppError>> { rows -> Ok(rows.map { it.toDomain() }) }
    .catch { emit(Err(AppError.Storage(it))) }

  override fun isFavorite(id: Int): Flow<Result<Boolean, AppError>> = dao.observeExists(id)
    .map<Boolean, Result<Boolean, AppError>> { Ok(it) }
    .catch { emit(Err(AppError.Storage(it))) }

  override suspend fun add(pokemon: PokemonSummary): Result<Unit, AppError> = catchingLocal {
    dao.upsert(FavoriteEntity(pokemon.id, pokemon.name, pokemon.spriteUrl, createdAt = now()))
  }

  override suspend fun remove(id: Int): Result<Unit, AppError> = catchingLocal { dao.delete(id) }
}

private fun FavoriteEntity.toDomain() =
  PokemonSummary(id = id, name = name, spriteUrl = spriteUrl, artworkUrl = artworkUrl(id))
