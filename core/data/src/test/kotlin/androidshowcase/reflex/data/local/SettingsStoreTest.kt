package androidshowcase.reflex.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import okio.Path.Companion.toPath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@OptIn(ExperimentalSerializationApi::class)
class SettingsStoreTest {
  @TempDir lateinit var dir: Path

  private val scopes = mutableListOf<CoroutineScope>()
  private val problems = mutableListOf<StorageProblem>()

  private val settingsFile get() = File(dir.toFile(), SETTINGS_FILE_NAME)

  /** DataStore allows one active instance per file: every store gets a scope the test cancels. */
  private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }

  private fun openStore(scope: CoroutineScope = newScope()) =
    createSettingsStore(dir.toString(), scope) { problems += it }

  @AfterEach
  fun cancelScopes() = scopes.forEach { it.cancel() }

  @Test
  fun `a value survives reopening the store`() = runBlocking {
    val first = newScope()
    openStore(first).updateData { SettingsProto(language = "vi", onboardingCompleted = true) }
    first.cancel()

    assertEquals(SettingsProto("vi", true), openStore().data.first())
    assertTrue(problems.isEmpty())
  }

  @Test
  fun `a garbage file yields the default, is kept aside and reported once`() = runBlocking {
    val garbage = byteArrayOf(0x0A, 0x7F, 0x01, 0x02, 0x03)
    settingsFile.writeBytes(garbage)

    assertEquals(SettingsProto(), openStore().data.first())

    assertEquals(1, problems.size)
    assertEquals(StorageProblem.Kind.CorruptFile, problems.single().kind)
    assertEquals(settingsFile.path, problems.single().path)
    assertArrayEquals(garbage, File(settingsFile.path + ".corrupt").readBytes())
  }

  @Test
  fun `a legacy preferences file is imported and deleted`() = runBlocking {
    val legacy = File(dir.toFile(), "settings.preferences_pb")
    val legacyScope = newScope()
    PreferenceDataStoreFactory.createWithPath(scope = legacyScope, produceFile = { legacy.path.toPath() })
      .edit {
        it[stringPreferencesKey("language")] = "vi"
        it[booleanPreferencesKey("onboarding_completed")] = true
      }
    legacyScope.cancel()

    assertEquals(SettingsProto("vi", true), openStore().data.first())

    assertFalse(legacy.exists())
    assertTrue(problems.isEmpty())
  }

  @Test
  fun `an unreadable legacy file is reported and skipped`() = runBlocking {
    val legacy = File(dir.toFile(), "settings.preferences_pb")
    legacy.writeBytes(byteArrayOf(0x0A, 0x7F, 0x01, 0x02, 0x03))

    assertEquals(SettingsProto(), openStore().data.first())

    assertEquals(listOf(StorageProblem.Kind.FailedMigration), problems.map { it.kind })
    assertFalse(legacy.exists())
    assertTrue(File(legacy.path + ".corrupt").exists())
  }

  @Test
  fun `a file from an older schema decodes with defaults`() = runBlocking {
    settingsFile.writeBytes(ProtoBuf.encodeToByteArray(SettingsV0.serializer(), SettingsV0("en")))

    assertEquals(SettingsProto(language = "en"), openStore().data.first())
    assertTrue(problems.isEmpty())
  }

  @Test
  fun `a file with an unknown field decodes`() = runBlocking {
    settingsFile.writeBytes(ProtoBuf.encodeToByteArray(SettingsV2.serializer(), SettingsV2("en", true, "x")))

    assertEquals(SettingsProto("en", true), openStore().data.first())
    assertTrue(problems.isEmpty())
  }

  @Serializable private data class SettingsV0(@ProtoNumber(1) val language: String? = null)

  @Serializable
  private data class SettingsV2(
    @ProtoNumber(1) val language: String? = null,
    @ProtoNumber(2) val onboardingCompleted: Boolean = false,
    @ProtoNumber(3) val theme: String? = null,
  )
}
