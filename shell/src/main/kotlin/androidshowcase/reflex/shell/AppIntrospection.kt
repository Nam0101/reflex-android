package androidshowcase.reflex.shell

import androidshowcase.reflex.feature.onboarding.LanguageViewModel
import androidshowcase.reflex.feature.onboarding.LanguageViewModelIntrospectionNode
import androidshowcase.reflex.feature.onboarding.OnboardingViewModel
import androidshowcase.reflex.feature.onboarding.OnboardingViewModelIntrospectionNode
import androidshowcase.reflex.feature.pokedex.FavoritesViewModel
import androidshowcase.reflex.feature.pokedex.FavoritesViewModelIntrospectionNode
import androidshowcase.reflex.feature.pokedex.PokedexViewModel
import androidshowcase.reflex.feature.pokedex.PokedexViewModelIntrospectionNode
import androidshowcase.reflex.feature.pokedex.PokemonDetailViewModel
import androidshowcase.reflex.feature.pokedex.PokemonDetailViewModelIntrospectionNode
import androidshowcase.reflex.introspection.IntrospectionAction
import androidshowcase.reflex.introspection.IntrospectionException
import androidshowcase.reflex.introspection.IntrospectionNode
import androidshowcase.reflex.introspection.IntrospectionSession
import androidshowcase.reflex.introspection.StackNavigatorNode
import androidshowcase.reflex.introspection.TabNavigatorNode
import androidshowcase.reflex.introspection.action
import androidshowcase.reflex.navigation.ScreenModel
import androidshowcase.reflex.navigation.StackNavigator
import androidshowcase.reflex.navigation.TabNavigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** The session the CLI, the behavior tests and the debug socket server talk to. */
fun ReflexApp.introspectionSession(): IntrospectionSession = IntrospectionSession { AppIntrospectionNode(this) }

/** Register the node of every new ViewModel here. */
internal fun resolveNode(model: ScreenModel): IntrospectionNode = when (model) {
  is StackNavigator -> StackNavigatorNode(model, ::resolveNode)
  is TabNavigator -> TabNavigatorNode(model, ::resolveNode)
  is SplashViewModel -> SplashViewModelIntrospectionNode(model)
  is LanguageViewModel -> LanguageViewModelIntrospectionNode(model)
  is OnboardingViewModel -> OnboardingViewModelIntrospectionNode(model)
  is PokedexViewModel -> PokedexViewModelIntrospectionNode(model)
  is PokemonDetailViewModel -> PokemonDetailViewModelIntrospectionNode(model)
  is FavoritesViewModel -> FavoritesViewModelIntrospectionNode(model)
  else -> error("No introspection node for ${model::class.simpleName}: register it in resolveNode")
}

private class AppIntrospectionNode(private val app: ReflexApp) : IntrospectionNode {
  @Serializable
  data class OpenDeepLinkInput(val uri: String)

  override val name = "App"
  override val state: JsonElement = JsonObject(emptyMap())
  override val actions = emptyList<IntrospectionAction>()

  /** Tapping a link is a user action, so the root lends it to whatever screen is focused. */
  override val lentActions = listOf(
    action("openDeepLink", "Open a deep link", OpenDeepLinkInput.serializer()) { input ->
      if (!app.openDeepLink(input.uri)) throw IntrospectionException("Unsupported deep link '${input.uri}'")
    },
  )

  override val child: IntrospectionNode = StackNavigatorNode(app.navigator, ::resolveNode)
}
