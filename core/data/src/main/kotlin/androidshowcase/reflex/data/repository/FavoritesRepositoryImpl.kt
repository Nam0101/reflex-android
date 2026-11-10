package androidshowcase.reflex.data.repository

import androidshowcase.reflex.data.catchingLocal
import androidshowcase.reflex.data.local.FavoriteDao
import androidshowcase.reflex.data.local.FavoriteEntity
import androidshowcase.reflex.data.remote.artworkUrl
import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.PokemonSummary
import androidshowcase.reflex.domain.repository.FavoritesRepository
import com.github.michaelbull.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** [dao] is lazy and first touched on [io], so building Room and loading SQLite never costs the main thread. */
class FavoritesRepositoryImpl(
  private val dao: Lazy<FavoriteDao>,
  private val io: CoroutineDispatcher = Dispatchers.IO,
  private val now: () -> Long = System::currentTimeMillis,
) : FavoritesRepository {
  override fun favorites(): Flow<Result<List<PokemonSummary>, AppError>> = flow { emitAll(dao.value.observeAll()) }
    .map { rows -> rows.map { it.toDomain() } }
    .catchingLocal()
    .flowOn(io)

  override fun isFavorite(id: Int): Flow<Result<Boolean, AppError>> = flow { emitAll(dao.value.observeExists(id)) }
    .catchingLocal()
    .flowOn(io)

  override suspend fun add(pokemon: PokemonSummary): Result<Unit, AppError> = withContext(io) {
    catchingLocal { dao.value.upsert(FavoriteEntity(pokemon.id, pokemon.name, pokemon.spriteUrl, createdAt = now())) }
  }

  override suspend fun remove(id: Int): Result<Unit, AppError> = withContext(io) {
    catchingLocal { dao.value.delete(id) }
  }
}

private fun FavoriteEntity.toDomain() =
  PokemonSummary(id = id, name = name, spriteUrl = spriteUrl, artworkUrl = artworkUrl(id))
