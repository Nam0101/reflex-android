package androidshowcase.reflex.data.repository

import androidshowcase.reflex.data.catchingRemote
import androidshowcase.reflex.data.remote.pokemon
import androidshowcase.reflex.data.remote.pokemonList
import androidshowcase.reflex.data.remote.toDomain
import androidshowcase.reflex.data.remote.toPage
import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.Page
import androidshowcase.reflex.domain.model.PokemonDetail
import androidshowcase.reflex.domain.model.PokemonSummary
import androidshowcase.reflex.domain.repository.PokemonRepository
import com.github.michaelbull.result.Result
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [client] is lazy and first touched on [io], so building the HTTP stack never costs the main thread. */
class PokemonRepositoryImpl(
  private val client: Lazy<HttpClient>,
  private val io: CoroutineDispatcher = Dispatchers.IO,
) : PokemonRepository {
  override suspend fun page(offset: Int, limit: Int): Result<Page<PokemonSummary>, AppError> = withContext(io) {
    catchingRemote { client.value.pokemonList(offset, limit).toPage(offset) }
  }

  override suspend fun detail(id: Int): Result<PokemonDetail, AppError> = withContext(io) {
    catchingRemote { client.value.pokemon(id).toDomain() }
  }
}
