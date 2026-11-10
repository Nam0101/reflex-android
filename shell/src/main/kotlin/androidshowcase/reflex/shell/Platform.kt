package androidshowcase.reflex.shell

import androidshowcase.reflex.data.local.ReflexDatabase
import androidshowcase.reflex.data.local.StorageProblem
import androidshowcase.reflex.data.remote.defaultHttpEngine
import androidshowcase.reflex.domain.platform.LanguageApplier
import androidx.room3.RoomDatabase
import io.ktor.client.engine.HttpClientEngine
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope

/**
 * Everything Core needs from the platform, passed to [ReflexApp]; `dataModule` binds it into the app's Koin instance.
 * Android passes real implementations; the CLI and behavior tests pass fixtures.
 * Factories are invoked lazily, off the main thread where it matters.
 */
class Platform(
  /** Where ViewModels run their effects: `Dispatchers.Main.immediate` on Android, one thread in the CLI. */
  val uiScope: CoroutineScope,
  val databaseBuilder: () -> RoomDatabase.Builder<ReflexDatabase>,
  /** Absolute path of the directory the typed stores keep their files in (`settings.pb`). */
  val storageDir: String,
  /** A store file that could not be read (corrupt, failed migration). The store went on with its defaults. */
  val onStorageProblem: (StorageProblem) -> Unit,
  val languageApplier: LanguageApplier,
  val httpEngine: () -> HttpClientEngine = ::defaultHttpEngine,
  val httpLogger: ((String) -> Unit)? = null,
  /** Time and chance are services too: the CLI and behavior tests pass a clock they move and a seeded random. */
  val clock: Clock = Clock.System,
  val random: Random = Random.Default,
)
