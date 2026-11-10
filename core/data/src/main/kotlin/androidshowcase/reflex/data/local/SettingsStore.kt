package androidshowcase.reflex.data.local

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

const val SETTINGS_FILE_NAME = "settings.pb"

/** The format before ProtoBuf: a Preferences DataStore file, imported once by [LegacySettingsMigration]. */
private const val LEGACY_SETTINGS_FILE_NAME = "settings.preferences_pb"

/**
 * The settings file. Evolution rules, because old files stay on devices:
 * - never reuse or renumber a [ProtoNumber]; removing a field retires its number;
 * - a new field gets the next free number and a default, so older files still decode;
 * - unknown fields (a file written by a newer build) are skipped on read.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class SettingsProto(
  @ProtoNumber(1) val language: String? = null,
  @ProtoNumber(2) val onboardingCompleted: Boolean = false,
)

internal fun createSettingsStore(
  directory: String,
  scope: CoroutineScope,
  onError: (StorageProblem) -> Unit,
): DataStore<SettingsProto> = createProtoStore(
  file = settingsFile(directory),
  serializer = SettingsProto.serializer(),
  default = SettingsProto(),
  scope = scope,
  onError = onError,
  migrations = listOf(LegacySettingsMigration("$directory/$LEGACY_SETTINGS_FILE_NAME".toPath(), onError)),
)

internal fun settingsFile(directory: String) = "$directory/$SETTINGS_FILE_NAME"

/** Imports `settings.preferences_pb` once, then deletes it. An unreadable one is reported and kept aside. */
private class LegacySettingsMigration(private val legacy: Path, private val onError: (StorageProblem) -> Unit) :
  DataMigration<SettingsProto> {
  override suspend fun shouldMigrate(currentData: SettingsProto) = FileSystem.SYSTEM.exists(legacy)

  override suspend fun migrate(currentData: SettingsProto): SettingsProto = try {
    val prefs = FileSystem.SYSTEM.read(legacy) { PreferencesSerializer.readFrom(this) }
    SettingsProto(
      language = prefs[stringPreferencesKey("language")],
      onboardingCompleted = prefs[booleanPreferencesKey("onboarding_completed")] ?: false,
    )
  } catch (e: Exception) {
    // A cancelled read cannot happen here (blocking I/O), but never swallow it if that changes.
    if (e is CancellationException) throw e
    keepUnreadable(legacy)
    onError(StorageProblem(StorageProblem.Kind.FailedMigration, legacy.toString(), e))
    currentData
  }

  override suspend fun cleanUp() {
    FileSystem.SYSTEM.delete(legacy, mustExist = false)
  }
}
