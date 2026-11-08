// The strict ViewModel contract (actions in, state out, one reducer) and navigation-as-state.
plugins {
  alias(libs.plugins.reflex.jvm.library)
}

dependencies {
  api(libs.kotlinx.coroutines.core)
}
