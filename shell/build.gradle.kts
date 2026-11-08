// The root of the object tree: wires services into ViewModels and owns the root navigator.
// Android (:app) and the CLI both build a ReflexApp; only the Platform they pass differs.
plugins {
  alias(libs.plugins.reflex.jvm.library)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  api(projects.core.data)
  api(projects.feature.onboarding)
  api(projects.feature.pokedex)
}
