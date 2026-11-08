package androidshowcase.reflex

import android.app.Application
import androidshowcase.reflex.shell.ReflexApp

/** Release twin of the debug introspection server: compiled out on purpose. */
internal object DebugTools {
  @Suppress("UNUSED_PARAMETER")
  fun install(application: Application, app: ReflexApp) = Unit
}
