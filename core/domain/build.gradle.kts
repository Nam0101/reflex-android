// Clean Architecture inner circle: entities, ports and use cases. Pure Kotlin, no framework.
plugins {
  alias(libs.plugins.reflex.jvm.library)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.serialization.core)
  api(libs.kotlin.result)
}
