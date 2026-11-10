pluginManagement {
  includeBuild("build-logic")
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "reflex"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Core: pure JVM modules (no android.*). They run headless on the JVM — CLI and behavior tests.
include(":core:domain")
include(":core:data")
include(":core:viewmodel")
include(":core:introspection")
include(":core:introspection-processor")
include(":feature:onboarding")
include(":feature:pokedex")
include(":shell")

// Mobile: Android + Compose. Views bind to Core ViewModels; platform services live here.
include(":core:designsystem")
include(":feature:onboarding-ui")
include(":feature:pokedex-ui")
include(":app")
include(":benchmark")

// CLI: drives Core headlessly (stdin/stdout NDJSON) or a debug build over a socket.
include(":cli")
