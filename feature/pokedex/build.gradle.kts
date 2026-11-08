// Core side of the pokedex feature: ViewModels, routes, navigation port, introspection nodes. Pure JVM.
plugins {
  alias(libs.plugins.reflex.jvm.library)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  api(projects.core.domain)
  api(projects.core.viewmodel)
  api(projects.core.introspection)
}
