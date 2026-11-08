package androidshowcase.reflex.navigation

/** A destination. Features declare their routes; the shell's [ScreenFactory] turns each into a model. */
interface Route

/** Anything a navigator hosts: a screen ViewModel (leaf) or another navigator (container). */
interface ScreenModel : AutoCloseable {
  /** Called once by the owning navigator after the screen is published. */
  fun start() {}

  override fun close() {}
}

fun interface ScreenFactory {
  fun create(route: Route): ScreenModel
}

/** A route that is on screen, with the model created for it. [key] is unique within its navigator. */
class Screen internal constructor(val key: String, val route: Route, val model: ScreenModel) {
  override fun toString() = "Screen($key, $route)"
}
