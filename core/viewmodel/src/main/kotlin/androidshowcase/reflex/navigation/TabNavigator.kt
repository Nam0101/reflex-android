package androidshowcase.reflex.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tabs as state. Every tab's model lives as long as the navigator, so switching tabs keeps their state. */
class TabNavigator(routes: List<Route>, factory: ScreenFactory, initialIndex: Int = 0) : ScreenModel {
  sealed interface Action {
    data class SelectTab(val index: Int) : Action
  }

  val tabs: List<Screen> = routes.mapIndexed { index, route -> Screen("tab#$index", route, factory.create(route)) }

  private val mutableSelectedIndex = MutableStateFlow(initialIndex)

  val selectedIndex: StateFlow<Int> = mutableSelectedIndex.asStateFlow()

  val selected: Screen get() = tabs[selectedIndex.value]

  init {
    require(initialIndex in tabs.indices) { "initialIndex $initialIndex out of ${tabs.indices}" }
  }

  fun send(action: Action) {
    when (action) {
      is Action.SelectTab -> {
        require(action.index in tabs.indices) { "No tab at ${action.index}" }
        mutableSelectedIndex.value = action.index
      }
    }
  }

  override fun start() = tabs.forEach { it.model.start() }

  override fun close() = tabs.forEach { it.model.close() }
}
