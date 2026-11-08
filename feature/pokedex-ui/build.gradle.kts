// Mobile side of the Pokédex: Compose views bound to the Core ViewModels of :feature:pokedex.
plugins {
  alias(libs.plugins.reflex.android.library)
  alias(libs.plugins.reflex.compose)
}

android {
  namespace = "androidshowcase.reflex.feature.pokedex.ui"
}

dependencies {
  api(projects.feature.pokedex)
  implementation(projects.core.designsystem)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.bundles.coil)
}
