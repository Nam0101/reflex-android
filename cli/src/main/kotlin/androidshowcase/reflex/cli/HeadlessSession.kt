package androidshowcase.reflex.cli

import androidshowcase.reflex.data.local.ReflexDatabase
import androidshowcase.reflex.data.local.StorageProblem
import androidshowcase.reflex.domain.model.AppLanguage
import androidshowcase.reflex.shell.Platform
import androidshowcase.reflex.shell.ReflexApp
import androidshowcase.reflex.shell.introspectionSession
import androidx.room3.Room
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

/**
 * The real app with fixtures at the edges: no device, no network, a fresh database and settings file
 * in a temp directory, a clock that stands still and a seeded random. ViewModel effects run on one "main"
 * thread, like on Android.
 */
class HeadlessSession(val fixtures: FixtureHttp = FixtureHttp()) : AutoCloseable {
  /** Where the app keeps its files. A test may plant or damage a file here before [relaunch]. */
  val dataDir: Path = Files.createTempDirectory("reflex-headless")
  private val mainThread = Executors.newSingleThreadExecutor { Thread(it, "reflex-main").apply { isDaemon = true } }
  private val mainDispatcher = mainThread.asCoroutineDispatcher()

  /** Time as the app sees it: `clock.advanceDays(1)` is tomorrow. It keeps its value across [relaunch]. */
  val clock = FakeClock()

  /** Languages the app asked the platform to apply, in order. */
  val appliedLanguages: MutableList<AppLanguage> = CopyOnWriteArrayList()

  /** Store files the app could not read. A behavior test fails on one it did not expect. */
  val storageProblems: MutableList<StorageProblem> = CopyOnWriteArrayList()

  private var scope = newScope()
  var app: ReflexApp = newApp()
    private set
  private var introspection = app.introspectionSession()

  /** One protocol line in, one out. See [androidshowcase.reflex.introspection.IntrospectionSession]. */
  fun handle(line: String): String = introspection.handle(line)

  /**
   * Calls the app on its main thread, where Android calls it from (`saveNavigation`, `restoreNavigation`,
   * `openDeepLink`). From any other thread such a call races with the navigation the app is doing.
   */
  fun <T> onMain(block: ReflexApp.() -> T): T = runBlocking(mainDispatcher) { app.block() }

  /** Kills and restarts the app on the same disk, like a process restart. */
  fun relaunch() {
    shutdown()
    scope = newScope()
    app = newApp()
    introspection = app.introspectionSession()
  }

  override fun close() {
    shutdown()
    fixtures.close()
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
      storageDir = dataDir.toString(),
      onStorageProblem = { storageProblems += it },
      languageApplier = { appliedLanguages += it },
      httpEngine = { fixtures.engine },
      clock = clock,
      // The same sequence on every launch: a journey that depends on chance is still reproducible.
      random = Random(RANDOM_SEED),
    ),
  )

  private companion object {
    const val RANDOM_SEED = 42
  }
}
