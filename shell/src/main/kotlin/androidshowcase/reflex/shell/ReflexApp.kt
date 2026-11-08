package androidshowcase.reflex.shell

import androidshowcase.reflex.domain.model.LaunchStep
import androidshowcase.reflex.domain.usecase.ResolveLaunchStep
import androidshowcase.reflex.feature.onboarding.LanguageRoute
import androidshowcase.reflex.feature.onboarding.OnboardingNavigation
import androidshowcase.reflex.feature.onboarding.OnboardingRoute
import androidshowcase.reflex.feature.onboarding.onboardingModule
import androidshowcase.reflex.feature.pokedex.FavoritesRoute
import androidshowcase.reflex.feature.pokedex.PokedexNavigation
import androidshowcase.reflex.feature.pokedex.PokedexRoute
import androidshowcase.reflex.feature.pokedex.PokemonDetailRoute
import androidshowcase.reflex.feature.pokedex.pokedexModule
import androidshowcase.reflex.navigation.Route
import androidshowcase.reflex.navigation.ScreenModel
import androidshowcase.reflex.navigation.StackNavigator
import androidshowcase.reflex.navigation.TabNavigator
import androidshowcase.reflex.navigation.UiScope
import androidshowcase.reflex.navigation.createScreen
import androidshowcase.reflex.navigation.screen
import java.util.concurrent.atomic.AtomicReference
import org.koin.dsl.koinApplication
import org.koin.dsl.module

data object HomeRoute : Route

/**
 * The app as an object tree: this root owns the services and the root [navigator]; navigators own
 * the screens on display; each screen is one ViewModel. Nothing here touches a view, so the whole app
 * can be walked headlessly. Views only render [navigator] and send actions.
 */
class ReflexApp(platform: Platform) : AutoCloseable {
  /** One Koin instance per app, never the global one: the CLI and the behavior tests run several apps. */
  private val koin = koinApplication {
    allowOverride(false)
    modules(dataModule(platform), shellModule(), onboardingModule(), pokedexModule())
  }.koin
  private val pendingDeepLink = AtomicReference<List<Route>?>(null)

  val navigator = StackNavigator(::createScreen)

  /** True until the first screen is decided; Android keeps the system splash up meanwhile. */
  val isLaunching: Boolean get() = navigator.screens.value.lastOrNull()?.route == SplashRoute

  private val onboardingNavigation = object : OnboardingNavigation {
    override fun languageChosen() = navigator.push(OnboardingRoute)

    override fun onboardingFinished() = goHome()
  }

  private val pokedexNavigation = PokedexNavigation { id -> navigator.push(PokemonDetailRoute(id)) }

  /** Returns false for links the app does not handle. Before Home is reachable the link waits for it. */
  fun openDeepLink(uri: String): Boolean {
    val routes = DeepLinks.parse(uri) ?: return false
    if (navigator.screens.value.firstOrNull()?.route == HomeRoute) {
      navigator.replaceAll(HomeRoute, *routes.toTypedArray())
    } else {
      pendingDeepLink.set(routes)
    }
    return true
  }

  override fun close() {
    navigator.close()
    koin.close()
  }

  private fun createScreen(route: Route): ScreenModel = koin.createScreen(route)

  /** What the shell itself provides: the features' navigation ports and the screens no feature owns. */
  private fun shellModule() = module {
    single<OnboardingNavigation> { onboardingNavigation }
    single<PokedexNavigation> { pokedexNavigation }
    screen<SplashRoute> { SplashViewModel(ResolveLaunchStep(get()), ::onLaunchStep, get(UiScope)) }
    screen<HomeRoute> { TabNavigator(listOf(PokedexRoute, FavoritesRoute), ::createScreen) }
  }

  private fun onLaunchStep(step: LaunchStep) = when (step) {
    LaunchStep.ChooseLanguage -> navigator.replaceAll(LanguageRoute)
    LaunchStep.Onboarding -> navigator.replaceAll(OnboardingRoute)
    LaunchStep.Home -> goHome()
  }

  private fun goHome() {
    val deepLink = pendingDeepLink.getAndSet(null).orEmpty()
    navigator.replaceAll(HomeRoute, *deepLink.toTypedArray())
  }

  // Last, so every property above is initialized before the first screen is created.
  init {
    navigator.replaceAll(SplashRoute)
  }
}
