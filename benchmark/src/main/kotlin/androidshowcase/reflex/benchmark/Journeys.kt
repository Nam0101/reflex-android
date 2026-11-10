package androidshowcase.reflex.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

internal const val PACKAGE = "androidshowcase.reflex"

private const val STEP_TIMEOUT_MS = 2_000L
private const val SCREEN_TIMEOUT_MS = 15_000L

// First launch is language (1 step) and onboarding (3 pages); the rest is slack for a slow device.
private const val FIRST_LAUNCH_STEPS = 10

// Selectors that do not depend on the UI language: the title is the same word in every language the app ships,
// the rest are the classes Compose reports for its semantics roles.
private val pokedexTitle = By.text("Pokédex")
private val radioButton = By.clazz("android.widget.RadioButton")
private val button = By.clazz("android.widget.Button")
private val scrollable = By.scrollable(true)

/** Starts the app and, on a first launch, picks the first language and pages through onboarding. */
internal fun MacrobenchmarkScope.launchToPokedex() {
  pressHome()
  startActivityAndWait()
  repeat(FIRST_LAUNCH_STEPS) {
    if (device.wait(Until.hasObject(pokedexTitle), STEP_TIMEOUT_MS)) return@repeat
    device.findObject(radioButton)?.click()
    // The primary button is the last one on both screens (onboarding has "Skip" above it).
    device.findObjects(button).lastOrNull()?.click()
  }
  check(device.wait(Until.hasObject(pokedexTitle), SCREEN_TIMEOUT_MS)) { "The Pokédex did not show up" }
  // Cards exist once the first page has arrived from the network.
  check(device.wait(Until.hasObject(scrollable), SCREEN_TIMEOUT_MS)) { "The Pokédex grid did not show up" }
}

internal fun MacrobenchmarkScope.flingPokedex() {
  val grid = checkNotNull(device.findObject(scrollable)) { "No Pokédex grid on screen" }
  // Stay clear of the system gesture areas at the screen edges.
  grid.setGestureMargin(device.displayWidth / 5)
  repeat(3) { grid.fling(Direction.DOWN) }
  grid.fling(Direction.UP)
  device.waitForIdle()
}

internal fun MacrobenchmarkScope.openFirstPokemonAndGoBack() {
  // Cards are the only clickable things inside the grid, whatever their content.
  val card = checkNotNull(device.findObject(scrollable)?.findObject(By.clickable(true))) { "No Pokémon card on screen" }
  card.click()
  check(device.wait(Until.gone(pokedexTitle), SCREEN_TIMEOUT_MS)) { "The detail screen did not open" }
  device.waitForIdle()
  device.pressBack()
  check(device.wait(Until.hasObject(pokedexTitle), SCREEN_TIMEOUT_MS)) { "Back did not return to the Pokédex" }
}
