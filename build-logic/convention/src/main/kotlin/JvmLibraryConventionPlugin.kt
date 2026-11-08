import androidshowcase.reflex.buildlogic.configureJUnit
import androidshowcase.reflex.buildlogic.configureKotlinJvm
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

/**
 * Plain Kotlin/JVM module. Core code lives here: android.* is not on the classpath, so a Core module
 * cannot reference the platform by construction, and it runs headless on the JVM (CLI, behavior tests).
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = with(target) {
    pluginManager.apply("org.jetbrains.kotlin.jvm")
    extensions.configure<JavaPluginExtension> {
      sourceCompatibility = JavaVersion.VERSION_21
      targetCompatibility = JavaVersion.VERSION_21
    }
    configureKotlinJvm()
    configureJUnit()
  }
}
