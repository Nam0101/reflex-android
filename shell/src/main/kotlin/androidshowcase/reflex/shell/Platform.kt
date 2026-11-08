package androidshowcase.reflex.shell

import androidshowcase.reflex.data.local.ReflexDatabase
import androidshowcase.reflex.data.remote.defaultHttpEngine
import androidshowcase.reflex.domain.platform.LanguageApplier
import androidx.room3.RoomDatabase
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope

/**
 * Everything Core needs from the platform, passed through the constructor (no DI framework).
 * Android passes real implementations; the CLI and behavior tests pass fixtures.
 * Factories are invoked lazily, off the main thread where it matters.
 */
class Platform(
  /** Where ViewModels run their effects: `Dispatchers.Main.immediate` on Android, one thread in the CLI. */
  val uiScope: CoroutineScope,
  val databaseBuilder: () -> RoomDatabase.Builder<ReflexDatabase>,
  /** Absolute path of the settings file, ending with `.preferences_pb`. */
  val settingsFile: String,
  val languageApplier: LanguageApplier,
  val httpEngine: () -> HttpClientEngine = ::defaultHttpEngine,
  val httpLogger: ((String) -> Unit)? = null,
)
