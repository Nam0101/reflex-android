import androidshowcase.reflex.buildlogic.ProjectConfig
import androidshowcase.reflex.buildlogic.configureKotlinJvm
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = with(target) {
    pluginManager.apply("com.android.library")
    extensions.configure<LibraryExtension> {
      compileSdk = ProjectConfig.COMPILE_SDK
      defaultConfig {
        minSdk = ProjectConfig.MIN_SDK
        testInstrumentationRunner = ProjectConfig.TEST_RUNNER
      }
      compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
      }
      // Only :app runs R8 over the whole graph; libraries never minify on their own.
      buildTypes.getByName("release").isMinifyEnabled = false
    }
    configureKotlinJvm()
  }
}
