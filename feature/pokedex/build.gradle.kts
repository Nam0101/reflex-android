// Core side of the pokedex feature: ViewModels, routes, navigation port; KSP generates the introspection nodes. Pure JVM.
plugins {
  alias(libs.plugins.reflex.jvm.library)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

dependencies {
  api(projects.core.domain)
  api(projects.core.viewmodel)
  api(projects.core.introspection)
  ksp(projects.core.introspectionProcessor)
}
