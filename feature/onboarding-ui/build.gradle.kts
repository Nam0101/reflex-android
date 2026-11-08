// Mobile side of onboarding: Compose views bound to the Core ViewModels of :feature:onboarding.
plugins {
  alias(libs.plugins.reflex.android.library)
  alias(libs.plugins.reflex.compose)
}

android {
  namespace = "androidshowcase.reflex.feature.onboarding.ui"
}

dependencies {
  api(projects.feature.onboarding)
  implementation(projects.core.designsystem)
  implementation(libs.androidx.lifecycle.runtime.compose)
}
