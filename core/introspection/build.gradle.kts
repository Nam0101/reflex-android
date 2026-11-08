// Introspection Kit: the contract between ViewModels and external drivers (CLI, behavior tests, a debug
// socket). The app never calls it; release builds strip it because nothing in the app references it.
plugins {
  alias(libs.plugins.reflex.jvm.library)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  api(projects.core.viewmodel)
  api(libs.kotlinx.serialization.json)
}
