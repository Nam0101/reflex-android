package androidshowcase.reflex.shell

import androidshowcase.reflex.feature.onboarding.onboardingModule
import androidshowcase.reflex.feature.pokedex.pokedexModule
import androidshowcase.reflex.navigation.Route
import androidshowcase.reflex.navigation.ScreenModel
import androidshowcase.reflex.navigation.StackNavigator
import androidshowcase.reflex.navigation.createScreen
import androidshowcase.reflex.navigation.routeTypes
import androidshowcase.reflex.viewmodel.EffectActivity
import org.koin.dsl.koinApplication

/**
 * The app as an object tree: this root owns the services and the root [navigator]; navigators own
 * the screens on display; each screen is one ViewModel. Nothing here touches a view, so the whole app
 * can be walked headlessly. Views only render [navigator] and send actions.
 *
 * It only wires the parts together: [AppNavigation] decides where the app goes, `shellModule` and the feature
 * modules create the screens, [NavigationSnapshots] carries the stack across process death.
 */
class ReflexApp(platform: Platform) : AutoCloseable {
  val navigator = StackNavigator(::createScreen)
  private val navigation = AppNavigation(navigator)

  private val effects = EffectActivity()

  /** One Koin instance per app, never the global one: the CLI and the behavior tests run several apps. */
  private val koin = koinApplication {
    allowOverride(false)
    modules(dataModule(platform, effects), shellModule(navigation), onboardingModule(), pokedexModule())
  }.koin
  private val snapshots = NavigationSnapshots(koin::routeTypes)

  /** True until the first screen is decided; Android keeps the system splash up meanwhile. */
  val isLaunching: Boolean get() = navigator.screens.value.lastOrNull()?.route == SplashRoute

  /** False while a task runs or a stream waits for its first value. Drivers wait on it instead of sleeping. */
  val isIdle: Boolean get() = effects.isIdle

  /** Returns false for links the app does not handle. Before Home is reachable the link waits for it. */
  fun openDeepLink(uri: String): Boolean {
    val routes = DeepLinks.parse(uri) ?: return false
    navigation.showOverHome(HomeStack(routes))
    return true
  }

  /**
   * The screens over Home and the selected tab, for the platform to keep while the process may be killed
   * (Android: saved instance state). Null before Home: a relaunch decides those screens from settings.
   */
  fun saveNavigation(): String? = navigation.homeStack()?.let(snapshots::write)

  /**
   * Reopens what [saveNavigation] returned in a process that was killed since. A snapshot this app wrote
   * itself is ignored (the screens are still alive), and so is one it cannot read (another app version).
   */
  fun restoreNavigation(saved: String) {
    snapshots.read(saved)?.let(navigation::showOverHome)
  }

  override fun close() {
    navigator.close()
    koin.close()
  }

  private fun createScreen(route: Route): ScreenModel = koin.createScreen(route)

  // Last, so every property above is initialized before the first screen is created.
  init {
    navigator.replaceAll(SplashRoute)
  }
}
