package androidshowcase.reflex.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class StackNavigatorTest {
  private data class Page(val name: String) : Route

  private class Model : ScreenModel {
    var started = 0
    var closed = 0

    override fun start() {
      started++
    }

    override fun close() {
      closed++
    }
  }

  private val models = mutableMapOf<Route, Model>()
  private val navigator = StackNavigator { route -> Model().also { models[route] = it } }

  @Test
  fun `push starts the new screen, pop closes it, the root is never popped`() {
    navigator.replaceAll(Page("a"))
    navigator.push(Page("b"))
    assertEquals(listOf(Page("a"), Page("b")), navigator.screens.value.map { it.route })
    assertEquals(1, models.getValue(Page("b")).started)

    navigator.send(StackNavigator.Action.GoBack)
    navigator.send(StackNavigator.Action.GoBack)
    assertEquals(listOf(Page("a")), navigator.screens.value.map { it.route })
    assertEquals(1, models.getValue(Page("b")).closed)
    assertFalse(navigator.canGoBack)
  }

  @Test
  fun `replaceAll closes every previous screen and keys stay unique`() {
    navigator.replaceAll(Page("a"))
    val a = models.getValue(Page("a"))
    navigator.replaceAll(Page("a"), Page("c"))
    assertEquals(1, a.closed)
    assertEquals(2, navigator.screens.value.map { it.key }.toSet().size)
  }
}
