import androidshowcase.reflex.buildlogic.ProjectConfig
import androidshowcase.reflex.buildlogic.configureKotlinJvm
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = with(target) {
    pluginManager.apply("com.android.application")
    extensions.configure<ApplicationExtension> {
      compileSdk = ProjectConfig.COMPILE_SDK
      defaultConfig {
        minSdk = ProjectConfig.MIN_SDK
        targetSdk = ProjectConfig.TARGET_SDK
        testInstrumentationRunner = ProjectConfig.TEST_RUNNER
      }
      compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
      }
      packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST,DEPENDENCIES}"
      }
    }
    configureKotlinJvm()
  }
}
