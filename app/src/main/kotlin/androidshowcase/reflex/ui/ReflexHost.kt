package androidshowcase.reflex.ui

import androidshowcase.reflex.R
import androidshowcase.reflex.designsystem.LocalSharedTransition
import androidshowcase.reflex.designsystem.SharedTransition
import androidshowcase.reflex.feature.onboarding.LanguageViewModel
import androidshowcase.reflex.feature.onboarding.OnboardingViewModel
import androidshowcase.reflex.feature.onboarding.ui.LanguageScreen
import androidshowcase.reflex.feature.onboarding.ui.OnboardingScreen
import androidshowcase.reflex.feature.pokedex.FavoritesRoute
import androidshowcase.reflex.feature.pokedex.FavoritesViewModel
import androidshowcase.reflex.feature.pokedex.PokedexViewModel
import androidshowcase.reflex.feature.pokedex.PokemonDetailViewModel
import androidshowcase.reflex.feature.pokedex.ui.FavoritesScreen
import androidshowcase.reflex.feature.pokedex.ui.PokedexScreen
import androidshowcase.reflex.feature.pokedex.ui.PokemonDetailScreen
import androidshowcase.reflex.navigation.Screen
import androidshowcase.reflex.navigation.StackNavigator
import androidshowcase.reflex.navigation.TabNavigator
import androidshowcase.reflex.shell.SplashViewModel
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay

/**
 * Renders the root navigator. The back stack is Core state: Navigation 3 only draws it (transitions,
 * predictive back) and reports back presses as an action. Each screen binds to its own model.
 */
@Composable
fun ReflexHost(navigator: StackNavigator) {
  val screens by navigator.screens.collectAsStateWithLifecycle()
  if (screens.isEmpty()) return
  val goBack = { navigator.send(StackNavigator.Action.GoBack) }
  SharedTransitionLayout {
    NavDisplay(
      backStack = screens,
      onBack = goBack,
      entryProvider = { screen ->
        NavEntry(screen, contentKey = screen.key) {
          // Screens hand elements over to the next screen (a Pokémon's artwork) through this.
          val animatedScope = LocalNavAnimatedContentScope.current
          val shared = remember(animatedScope) { SharedTransition(this@SharedTransitionLayout, animatedScope) }
          CompositionLocalProvider(LocalSharedTransition provides shared) { ScreenContent(it, goBack) }
        }
      },
    )
  }
}

@Composable
private fun ScreenContent(screen: Screen, onBack: () -> Unit) {
  when (val model = screen.model) {
    // The system splash screen covers this until the first real screen replaces it.
    is SplashViewModel -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))

    is LanguageViewModel -> LanguageScreen(model)

    is OnboardingViewModel -> OnboardingScreen(model)

    is TabNavigator -> HomeTabs(model)

    is PokemonDetailViewModel -> PokemonDetailScreen(model, onBack)

    else -> error("No view registered for ${screen.route}")
  }
}

@Composable
private fun HomeTabs(tabs: TabNavigator) {
  val selectedIndex by tabs.selectedIndex.collectAsStateWithLifecycle()
  // Keeps each tab's scroll position while another tab is shown.
  val saveableStateHolder = rememberSaveableStateHolder()
  Scaffold(
    bottomBar = {
      NavigationBar {
        tabs.tabs.forEachIndexed { index, tab ->
          val favorites = tab.route == FavoritesRoute
          NavigationBarItem(
            selected = index == selectedIndex,
            onClick = { tabs.send(TabNavigator.Action.SelectTab(index)) },
            icon = { Icon(if (favorites) Icons.Filled.Favorite else Icons.Filled.Search, contentDescription = null) },
            label = { Text(stringResource(if (favorites) R.string.tab_favorites else R.string.tab_pokedex)) },
          )
        }
      }
    },
  ) { padding ->
    val screen = tabs.tabs[selectedIndex]
    Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
      saveableStateHolder.SaveableStateProvider(screen.key) {
        when (val model = screen.model) {
          is PokedexViewModel -> PokedexScreen(model)
          is FavoritesViewModel -> FavoritesScreen(model)
          else -> error("No view registered for tab ${screen.route}")
        }
      }
    }
  }
}
