import androidshowcase.reflex.buildlogic.lib
import androidshowcase.reflex.buildlogic.libs
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Compose for an Android module: compiler plugin, stability config and the BOM-aligned UI basics. */
class ComposeConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = with(target) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

    // AGP 9.5 renders @Preview through a Gradle task that needs compiled resources in unit tests.
    extensions.findByType<ApplicationExtension>()?.apply {
      buildFeatures.compose = true
      testOptions.unitTests.isIncludeAndroidResources = true
    }
    extensions.findByType<LibraryExtension>()?.apply {
      buildFeatures.compose = true
      testOptions.unitTests.isIncludeAndroidResources = true
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
      stabilityConfigurationFiles.add(
        isolated.rootProject.projectDirectory.file("config/compose/compose_compiler_config.conf"),
      )
      // ./gradlew assembleRelease -PenableComposeCompilerReports → build/compose_compiler per module.
      if (providers.gradleProperty("enableComposeCompilerReports").isPresent) {
        metricsDestination.set(layout.buildDirectory.dir("compose_compiler"))
        reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
      }
    }

    dependencies {
      val bom = platform(libs.lib("androidx-compose-bom"))
      add("implementation", bom)
      add("implementation", libs.lib("androidx-compose-ui"))
      add("implementation", libs.lib("androidx-compose-foundation"))
      add("implementation", libs.lib("androidx-compose-material3"))
      add("implementation", libs.lib("androidx-compose-ui-tooling-preview"))
      add("debugImplementation", libs.lib("androidx-compose-ui-tooling"))
    }
  }
}
