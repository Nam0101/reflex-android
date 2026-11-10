// The strict ViewModel contract (actions in, state out, one reducer) and navigation-as-state.
plugins {
  alias(libs.plugins.reflex.jvm.library)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.serialization.core)
  api(libs.koin.core)
}
