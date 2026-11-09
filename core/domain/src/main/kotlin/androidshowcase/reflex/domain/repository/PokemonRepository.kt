package androidshowcase.reflex.domain.repository

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.Page
import androidshowcase.reflex.domain.model.PokemonDetail
import androidshowcase.reflex.domain.model.PokemonSummary
import com.github.michaelbull.result.Result

interface PokemonRepository {
  suspend fun page(offset: Int, limit: Int): Result<Page<PokemonSummary>, AppError>

  suspend fun detail(id: Int): Result<PokemonDetail, AppError>

  /** Official artwork of the Pokémon [id]: known before its detail loads, so a screen can show it at once. */
  fun artworkUrl(id: Int): String
}
