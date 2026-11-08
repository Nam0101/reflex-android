package androidshowcase.reflex.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal fun Project.configureKotlinJvm() {
  tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_21)
    }
  }
}

/** JUnit 6 (Jupiter) for JVM unit tests. Gradle 9 needs the platform launcher on the test runtime. */
internal fun Project.configureJUnit() {
  dependencies {
    add("testImplementation", platform(libs.lib("junit-bom")))
    add("testImplementation", libs.lib("junit-jupiter"))
    add("testImplementation", libs.lib("kotlinx-coroutines-test"))
    add("testRuntimeOnly", libs.lib("junit-platform-launcher"))
  }
  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
  }
}
