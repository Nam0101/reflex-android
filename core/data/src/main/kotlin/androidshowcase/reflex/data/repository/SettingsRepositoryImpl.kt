package androidshowcase.reflex.data.repository

import androidshowcase.reflex.data.catchingLocal
import androidshowcase.reflex.data.local.createSettingsDataStore
import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.AppLanguage
import androidshowcase.reflex.domain.model.UserSettings
import androidshowcase.reflex.domain.repository.SettingsRepository
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.michaelbull.result.Result
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {
  override val settings: Flow<UserSettings> = dataStore.data
    // A read error (not corruption, which the store's handler repairs) falls back to first-launch
    // settings: the user is asked again rather than the app crashing on start.
    .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
    .map { prefs ->
      UserSettings(
        language = AppLanguage.fromTag(prefs[LANGUAGE]),
        onboardingCompleted = prefs[ONBOARDING_COMPLETED] ?: false,
      )
    }
    .distinctUntilChanged()

  override suspend fun setLanguage(language: AppLanguage): Result<Unit, AppError> = catchingLocal {
    dataStore.edit { it[LANGUAGE] = language.tag }
  }

  override suspend fun completeOnboarding(): Result<Unit, AppError> = catchingLocal {
    dataStore.edit { it[ONBOARDING_COMPLETED] = true }
  }

  private companion object {
    val LANGUAGE = stringPreferencesKey("language")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
  }
}

/**
 * Settings stored with DataStore Preferences at [absolutePath] (`*.preferences_pb`).
 * Cancel [scope] to release the file.
 */
fun createSettingsRepository(absolutePath: String, scope: CoroutineScope): SettingsRepository =
  SettingsRepositoryImpl(createSettingsDataStore(absolutePath, scope))
