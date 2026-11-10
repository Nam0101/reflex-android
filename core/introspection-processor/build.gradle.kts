// KSP processor: generates the introspection node of every @Introspected ViewModel and registers it for the
// shell (ServiceLoader). Runs at build time only; nothing here ships in the app.
plugins {
  alias(libs.plugins.reflex.jvm.library)
}

dependencies {
  implementation(libs.ksp.api)
}
