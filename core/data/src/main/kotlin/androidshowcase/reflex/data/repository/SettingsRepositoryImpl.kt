package androidshowcase.reflex.data.repository

import androidshowcase.reflex.data.catchingLocal
import androidshowcase.reflex.data.local.SettingsProto
import androidshowcase.reflex.data.local.StorageProblem
import androidshowcase.reflex.data.local.createSettingsStore
import androidshowcase.reflex.data.local.settingsFile
import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.AppLanguage
import androidshowcase.reflex.domain.model.UserSettings
import androidshowcase.reflex.domain.repository.SettingsRepository
import androidx.datastore.core.DataStore
import com.github.michaelbull.result.Result
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class SettingsRepositoryImpl(
  private val dataStore: DataStore<SettingsProto>,
  file: String,
  onError: (StorageProblem) -> Unit,
) : SettingsRepository {
  override val settings: Flow<UserSettings> = dataStore.data
    // A read error (not corruption, which the store repairs and reports) falls back to first-launch
    // settings: the user is asked again rather than the app crashing on start.
    .catch {
      if (it !is IOException) throw it
      onError(StorageProblem(StorageProblem.Kind.Unreadable, file, it))
      emit(SettingsProto())
    }
    .map { UserSettings(language = AppLanguage.fromTag(it.language), onboardingCompleted = it.onboardingCompleted) }
    .distinctUntilChanged()

  override suspend fun setLanguage(language: AppLanguage): Result<Unit, AppError> = catchingLocal {
    dataStore.updateData { it.copy(language = language.tag) }
  }

  override suspend fun completeOnboarding(): Result<Unit, AppError> = catchingLocal {
    dataStore.updateData { it.copy(onboardingCompleted = true) }
  }
}

/**
 * Settings stored as ProtoBuf in [SETTINGS_FILE_NAME][androidshowcase.reflex.data.local.SETTINGS_FILE_NAME]
 * inside [directory]. Storage problems are recovered from and reported to [onError]. Cancel [scope] to
 * release the file.
 */
fun createSettingsRepository(
  directory: String,
  scope: CoroutineScope,
  onError: (StorageProblem) -> Unit,
): SettingsRepository {
  val file = settingsFile(directory)
  return SettingsRepositoryImpl(createSettingsStore(directory, scope, onError), file, onError)
}
