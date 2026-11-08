plugins {
  alias(libs.plugins.reflex.android.library)
  alias(libs.plugins.reflex.compose)
}

android {
  namespace = "androidshowcase.reflex.designsystem"
}

dependencies {
  api(projects.core.domain)
  api(libs.androidx.compose.material.icons.core)
}
