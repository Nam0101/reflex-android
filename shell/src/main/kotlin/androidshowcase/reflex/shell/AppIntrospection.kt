package androidshowcase.reflex.shell

import androidshowcase.reflex.introspection.IntrospectionAction
import androidshowcase.reflex.introspection.IntrospectionException
import androidshowcase.reflex.introspection.IntrospectionNode
import androidshowcase.reflex.introspection.IntrospectionNodeProvider
import androidshowcase.reflex.introspection.IntrospectionSession
import androidshowcase.reflex.introspection.StackNavigatorNode
import androidshowcase.reflex.introspection.TabNavigatorNode
import androidshowcase.reflex.introspection.action
import androidshowcase.reflex.navigation.ScreenModel
import androidshowcase.reflex.navigation.StackNavigator
import androidshowcase.reflex.navigation.TabNavigator
import java.util.ServiceLoader
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** The session the CLI, the behavior tests and the debug socket server talk to. */
fun ReflexApp.introspectionSession(): IntrospectionSession =
  IntrospectionSession(isIdle = { isIdle }) { AppIntrospectionNode(this) }

/** One generated provider per module with `@Introspected` ViewModels; nothing is registered by hand. */
private val nodeProviders: List<IntrospectionNodeProvider> by lazy {
  ServiceLoader.load(IntrospectionNodeProvider::class.java, IntrospectionNodeProvider::class.java.classLoader).toList()
}

internal fun resolveNode(model: ScreenModel): IntrospectionNode = when (model) {
  is StackNavigator -> StackNavigatorNode(model, ::resolveNode)

  is TabNavigator -> TabNavigatorNode(model, ::resolveNode)

  else -> nodeProviders.firstNotNullOfOrNull { it.nodeFor(model) }
    ?: error("No introspection node for ${model::class.simpleName}: annotate it with @Introspected")
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
