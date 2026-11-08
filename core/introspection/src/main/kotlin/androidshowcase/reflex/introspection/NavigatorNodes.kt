package androidshowcase.reflex.introspection

import androidshowcase.reflex.navigation.Route
import androidshowcase.reflex.navigation.ScreenModel
import androidshowcase.reflex.navigation.StackNavigator
import androidshowcase.reflex.navigation.TabNavigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Resolves the node of any model a navigator hosts. The shell provides it (it knows every ViewModel). */
typealias NodeResolver = (ScreenModel) -> IntrospectionNode

class StackNavigatorNode(private val navigator: StackNavigator, private val resolve: NodeResolver) : IntrospectionNode {
  private val goBack = action("goBack", "Go back") { navigator.send(StackNavigator.Action.GoBack) }

  override val name = "Stack"

  override val state: JsonElement
    get() = buildJsonObject {
      putJsonArray("routes") { navigator.screens.value.forEach { add(it.route.toString()) } }
    }

  override val actions = emptyList<IntrospectionAction>()

  override val lentActions: List<IntrospectionAction>
    get() = if (navigator.canGoBack) listOf(goBack) else emptyList()

  override val child: IntrospectionNode?
    get() = navigator.screens.value.lastOrNull()?.let { resolve(it.model) }
}

class TabNavigatorNode(private val navigator: TabNavigator, private val resolve: NodeResolver) : IntrospectionNode {
  @Serializable
  data class SelectTabInput(val tab: String)

  private val tabNames = navigator.tabs.map { it.route.tabName() }

  private val selectTab = action("selectTab", "Select tab", SelectTabInput.serializer()) { input ->
    val index = tabNames.indexOf(input.tab)
    if (index < 0) throw IntrospectionException("Unknown tab '${input.tab}'. Tabs: ${tabNames.joinToString()}")
    navigator.send(TabNavigator.Action.SelectTab(index))
  }

  override val name = "Tabs"

  override val state: JsonElement
    get() = buildJsonObject {
      putJsonArray("tabs") { tabNames.forEach { add(it) } }
      put("selected", tabNames[navigator.selectedIndex.value])
    }

  override val actions = emptyList<IntrospectionAction>()

  override val lentActions = listOf(selectTab)

  override val child: IntrospectionNode get() = resolve(navigator.selected.model)
}

/** `PokedexRoute` → `Pokedex`. */
private fun Route.tabName() = toString().removeSuffix("Route")
