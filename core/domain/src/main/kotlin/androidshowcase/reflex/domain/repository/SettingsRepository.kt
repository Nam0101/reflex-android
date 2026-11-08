package androidshowcase.reflex.domain.repository

import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.AppLanguage
import androidshowcase.reflex.domain.model.UserSettings
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
  /** The first emission is the persisted value read from disk, never a fabricated default. */
  val settings: Flow<UserSettings>

  /** Suspends until the value is durably written. */
  suspend fun setLanguage(language: AppLanguage): Result<Unit, AppError>

  suspend fun completeOnboarding(): Result<Unit, AppError>
}
