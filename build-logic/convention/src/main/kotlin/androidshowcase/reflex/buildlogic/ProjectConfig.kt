package androidshowcase.reflex.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

object ProjectConfig {
  const val MIN_SDK = 26
  const val TARGET_SDK = 37
  const val COMPILE_SDK = 37
  const val TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
}

internal val Project.libs: VersionCatalog
  get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()
