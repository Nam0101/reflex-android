package androidshowcase.reflex.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Push navigation as state: [screens] is the back stack, top last. Pushing creates the screen's model
 * through the [ScreenFactory]; popping closes it. Views render [screens] and only ever call [send];
 * ViewModels navigate through ports the shell implements with [push] / [replaceAll].
 */
class StackNavigator(private val factory: ScreenFactory) : ScreenModel {
  sealed interface Action {
    data object GoBack : Action
  }

  private val lock = Any()
  private var nextKey = 0L
  private val mutableScreens = MutableStateFlow<List<Screen>>(emptyList())

  val screens: StateFlow<List<Screen>> = mutableScreens.asStateFlow()

  val canGoBack: Boolean get() = screens.value.size > 1

  fun send(action: Action) {
    when (action) {
      Action.GoBack -> pop()
    }
  }

  fun push(route: Route) = mutate { it + screen(route) }

  /** Never empties the stack: at the root, back belongs to the platform (Android finishes the task). */
  fun pop() = mutate { if (it.size > 1) it.dropLast(1) else it }

  fun replaceAll(vararg routes: Route) = mutate { routes.map(::screen) }

  override fun close() = synchronized(lock) { mutableScreens.value }.forEach { it.model.close() }

  private fun screen(route: Route) = Screen("${route::class.simpleName}#${nextKey++}", route, factory.create(route))

  private fun mutate(transform: (List<Screen>) -> List<Screen>) {
    val (added, removed) = synchronized(lock) {
      val old = mutableScreens.value
      val new = transform(old)
      mutableScreens.value = new
      new.filterNot { it in old } to old.filterNot { it in new }
    }
    // Outside the lock: closing or starting a screen may navigate again.
    removed.forEach { it.model.close() }
    added.forEach { it.model.start() }
  }
}
