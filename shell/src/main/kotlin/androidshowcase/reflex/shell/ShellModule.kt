package androidshowcase.reflex.shell

import androidshowcase.reflex.domain.usecase.ResolveLaunchStep
import androidshowcase.reflex.feature.onboarding.OnboardingNavigation
import androidshowcase.reflex.feature.pokedex.FavoritesRoute
import androidshowcase.reflex.feature.pokedex.PokedexNavigation
import androidshowcase.reflex.feature.pokedex.PokedexRoute
import androidshowcase.reflex.navigation.TabNavigator
import androidshowcase.reflex.navigation.UiScope
import androidshowcase.reflex.navigation.createScreen
import androidshowcase.reflex.navigation.screen
import org.koin.core.module.Module
import org.koin.dsl.module

/** What the shell itself provides: the features' navigation ports and the screens no feature owns. */
internal fun shellModule(navigation: AppNavigation): Module = module {
  single<OnboardingNavigation> { navigation }
  single<PokedexNavigation> { navigation }
  screen<SplashRoute> { SplashViewModel(ResolveLaunchStep(get()), navigation::onLaunchStep, get(UiScope)) }
  screen<HomeRoute> {
    val koin = getKoin()
    TabNavigator(listOf(PokedexRoute, FavoritesRoute), koin::createScreen)
  }
}
