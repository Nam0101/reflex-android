plugins {
  `kotlin-dsl`
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}

dependencies {
  compileOnly(libs.android.gradlePlugin)
  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
  plugins {
    register("jvmLibrary") {
      id = libs.plugins.reflex.jvm.library.get().pluginId
      implementationClass = "JvmLibraryConventionPlugin"
    }
    register("androidApplication") {
      id = libs.plugins.reflex.android.application.get().pluginId
      implementationClass = "AndroidApplicationConventionPlugin"
    }
    register("androidLibrary") {
      id = libs.plugins.reflex.android.library.get().pluginId
      implementationClass = "AndroidLibraryConventionPlugin"
    }
    register("compose") {
      id = libs.plugins.reflex.compose.get().pluginId
      implementationClass = "ComposeConventionPlugin"
    }
  }
}
