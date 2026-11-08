package androidshowcase.reflex.cli

import androidshowcase.reflex.data.local.ReflexDatabase
import androidshowcase.reflex.data.local.SETTINGS_FILE_NAME
import androidshowcase.reflex.domain.model.AppLanguage
import androidshowcase.reflex.shell.Platform
import androidshowcase.reflex.shell.ReflexApp
import androidshowcase.reflex.shell.introspectionSession
import androidx.room3.Room
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel

/**
 * The real app with fixtures at the edges: no device, no network, a fresh database and settings file
 * in a temp directory. ViewModel effects run on one "main" thread, like on Android.
 */
class HeadlessSession(val fixtures: FixtureHttp = FixtureHttp()) : AutoCloseable {
  private val dataDir = Files.createTempDirectory("reflex-headless")
  private val mainThread = Executors.newSingleThreadExecutor { Thread(it, "reflex-main").apply { isDaemon = true } }
  private val mainDispatcher = mainThread.asCoroutineDispatcher()

  /** Languages the app asked the platform to apply, in order. */
  val appliedLanguages: MutableList<AppLanguage> = CopyOnWriteArrayList()

  private var scope = newScope()
  var app: ReflexApp = newApp()
    private set
  private var introspection = app.introspectionSession()

  /** One protocol line in, one out. See [androidshowcase.reflex.introspection.IntrospectionSession]. */
  fun handle(line: String): String = introspection.handle(line)

  /** Kills and restarts the app on the same disk, like a process restart. */
  fun relaunch() {
    shutdown()
    scope = newScope()
    app = newApp()
    introspection = app.introspectionSession()
  }

  override fun close() {
    shutdown()
    mainDispatcher.close()
    dataDir.toFile().deleteRecursively()
  }

  private fun shutdown() {
    app.close()
    scope.cancel()
  }

  private fun newScope() = CoroutineScope(SupervisorJob() + mainDispatcher)

  private fun newApp() = ReflexApp(
    Platform(
      uiScope = scope,
      databaseBuilder = { Room.databaseBuilder<ReflexDatabase>(dataDir.resolve(ReflexDatabase.FILE_NAME).toString()) },
      settingsFile = dataDir.resolve(SETTINGS_FILE_NAME).toString(),
      languageApplier = { appliedLanguages += it },
      httpEngine = { fixtures.engine },
    ),
  )
}
