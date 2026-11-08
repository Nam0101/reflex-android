package androidshowcase.reflex.shell

import androidshowcase.reflex.domain.model.LaunchStep
import androidshowcase.reflex.domain.usecase.ResolveLaunchStep
import androidshowcase.reflex.feature.onboarding.LanguageRoute
import androidshowcase.reflex.feature.onboarding.LanguageViewModel
import androidshowcase.reflex.feature.onboarding.OnboardingNavigation
import androidshowcase.reflex.feature.onboarding.OnboardingRoute
import androidshowcase.reflex.feature.onboarding.OnboardingViewModel
import androidshowcase.reflex.feature.pokedex.FavoritesRoute
import androidshowcase.reflex.feature.pokedex.FavoritesViewModel
import androidshowcase.reflex.feature.pokedex.PokedexNavigation
import androidshowcase.reflex.feature.pokedex.PokedexRoute
import androidshowcase.reflex.feature.pokedex.PokedexViewModel
import androidshowcase.reflex.feature.pokedex.PokemonDetailRoute
import androidshowcase.reflex.feature.pokedex.PokemonDetailViewModel
import androidshowcase.reflex.navigation.Route
import androidshowcase.reflex.navigation.ScreenModel
import androidshowcase.reflex.navigation.StackNavigator
import androidshowcase.reflex.navigation.TabNavigator
import java.util.concurrent.atomic.AtomicReference

data object HomeRoute : Route

/**
 * The app as an object tree: this root owns the services and the root [navigator]; navigators own
 * the screens on display; each screen is one ViewModel. Nothing here touches a view, so the whole app
 * can be walked headlessly. Views only render [navigator] and send actions.
 */
class ReflexApp(private val platform: Platform) : AutoCloseable {
  private val graph = AppGraph(platform)
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
    graph.close()
  }

  private fun createScreen(route: Route): ScreenModel = when (route) {
    SplashRoute -> SplashViewModel(ResolveLaunchStep(graph.settings), ::onLaunchStep, platform.uiScope)
    LanguageRoute -> LanguageViewModel(graph.settings, platform.languageApplier, onboardingNavigation, platform.uiScope)
    OnboardingRoute -> OnboardingViewModel(graph.settings, onboardingNavigation, platform.uiScope)
    HomeRoute -> TabNavigator(listOf(PokedexRoute, FavoritesRoute), ::createScreen)
    PokedexRoute -> PokedexViewModel(graph.pokemon, pokedexNavigation, platform.uiScope)
    FavoritesRoute -> FavoritesViewModel(graph.favorites, pokedexNavigation, platform.uiScope)
    is PokemonDetailRoute -> PokemonDetailViewModel(route.id, graph.pokemon, graph.favorites, platform.uiScope)
    else -> error("No screen registered for $route")
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
