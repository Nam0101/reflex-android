package androidshowcase.reflex.domain.repository

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.Page
import androidshowcase.reflex.domain.model.PokemonDetail
import androidshowcase.reflex.domain.model.PokemonSummary
import com.github.michaelbull.result.Result

interface PokemonRepository {
  suspend fun page(offset: Int, limit: Int): Result<Page<PokemonSummary>, AppError>

  suspend fun detail(id: Int): Result<PokemonDetail, AppError>
}
