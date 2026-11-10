// Clean Architecture adapters: Ktor, Room 3 and DataStore behind the domain ports. Pure JVM, so the
// real data layer also runs headless (CLI, behavior tests) with fixtures only at the edges.
plugins {
  alias(libs.plugins.reflex.jvm.library)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.room3)
}

room3 {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  api(projects.core.domain)

  api(libs.ktor.client.core)
  // api: platforms build the OkHttpClient they share between the API client and their image loader.
  api(libs.ktor.client.okhttp)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.client.logging)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.kotlinx.serialization.json)

  api(libs.androidx.room3.runtime)
  implementation(libs.androidx.sqlite.bundled)
  ksp(libs.androidx.room3.compiler)
  implementation(libs.androidx.datastore.preferences.core)

  testImplementation(libs.ktor.client.mock)
}
