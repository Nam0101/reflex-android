package androidshowcase.reflex.shell

import androidshowcase.reflex.domain.model.LaunchStep
import androidshowcase.reflex.feature.onboarding.LanguageRoute
import androidshowcase.reflex.feature.onboarding.OnboardingNavigation
import androidshowcase.reflex.feature.onboarding.OnboardingRoute
import androidshowcase.reflex.feature.pokedex.PokedexNavigation
import androidshowcase.reflex.feature.pokedex.PokemonDetailRoute
import androidshowcase.reflex.navigation.Route
import androidshowcase.reflex.navigation.StackNavigator
import androidshowcase.reflex.navigation.TabNavigator
import java.util.concurrent.atomic.AtomicReference
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : Route

/** What is shown over Home, and the tab to select (null keeps the current one). */
internal class HomeStack(val routes: List<Route>, val tab: Int? = null)

/**
 * Where the app goes. It implements the navigation ports of every feature, so no feature knows what comes
 * next, and it owns the rule that deep links and restored screens open over Home.
 */
internal class AppNavigation(private val navigator: StackNavigator) :
  OnboardingNavigation,
  PokedexNavigation {
  private val pendingHomeStack = AtomicReference<HomeStack?>(null)

  private val home: TabNavigator? get() = navigator.screens.value.firstOrNull()?.model as? TabNavigator

  override fun languageChosen() = navigator.push(OnboardingRoute)

  override fun onboardingFinished() = goHome()

  override fun openPokemon(id: Int) = navigator.push(PokemonDetailRoute(id))

  fun onLaunchStep(step: LaunchStep) = when (step) {
    LaunchStep.ChooseLanguage -> navigator.replaceAll(LanguageRoute)
    LaunchStep.Onboarding -> navigator.replaceAll(OnboardingRoute)
    LaunchStep.Home -> goHome()
  }

  /** Home stays alive with its state; before Home is reachable the stack waits for it. */
  fun showOverHome(stack: HomeStack) {
    if (home != null) {
      navigator.replaceAboveRoot(*stack.routes.toTypedArray())
      selectTab(stack.tab)
    } else {
      pendingHomeStack.set(stack)
    }
  }

  /** The screens over Home and the selected tab; null before Home. */
  fun homeStack(): HomeStack? {
    val screens = navigator.screens.value
    val home = screens.firstOrNull()?.model as? TabNavigator ?: return null
    return HomeStack(screens.drop(1).map { it.route }, home.selectedIndex.value)
  }

  private fun goHome() {
    val stack = pendingHomeStack.getAndSet(null)
    navigator.replaceAll(HomeRoute, *stack?.routes.orEmpty().toTypedArray())
    selectTab(stack?.tab)
  }

  private fun selectTab(index: Int?) {
    val home = home ?: return
    if (index != null && index in home.tabs.indices) home.send(TabNavigator.Action.SelectTab(index))
  }
}
