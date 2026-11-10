package androidshowcase.reflex.cli

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The registrations the compiler cannot see, checked on the sources: a screen that is missing one fails here,
 * at build time, instead of crashing ("No view registered…") when someone opens it.
 */
class WiringTest {
  private val root = generateSequence(File("").absoluteFile) {
    it.parentFile
  }.first { File(it, "settings.gradle.kts").exists() }
  private val coreSources = (File(root, "feature").listFiles().orEmpty().toList() + File(root, "shell"))
    .flatMap { File(it, "src/main/kotlin").walkTopDown().filter { file -> file.extension == "kt" } }
    .map { it.readText() }
  private val viewModels = coreSources.flatMap { VIEW_MODEL.findAll(it).map { match -> match.groupValues[1] } }

  @Test
  fun `every screen ViewModel is created by a Koin screen and has a view in ReflexHost`() {
    assertTrue(viewModels.isNotEmpty()) { "No @Introspected ViewModel found under $root: did the layout change?" }
    val host = File(root, "app/src/main/kotlin").walkTopDown().first { it.name == "ReflexHost.kt" }.readText()
    val problems = viewModels.flatMap { viewModel ->
      listOfNotNull(
        "$viewModel: not created in any Koin screen<Route> { } block".takeUnless {
          coreSources.any { Regex("""screen<\w+>\s*\{[^}]*\b$viewModel\(""").containsMatchIn(it) }
        },
        "$viewModel: no 'is $viewModel ->' view branch in ReflexHost.kt".takeUnless { "is $viewModel ->" in host },
      )
    }
    assertEquals(emptyList<String>(), problems)
  }

  @Test
  fun `every string has a Vietnamese twin`() {
    val problems = root.walkTopDown()
      .onEnter { it.name != "build" && !it.name.startsWith(".") }
      .filter { it.path.endsWith("src/main/res/values/strings.xml") }
      .flatMap { english ->
        val vietnamese = File(english.parentFile.parentFile, "values-vi/strings.xml")
        val names = stringNames(english)
        val twins = if (vietnamese.exists()) stringNames(vietnamese) else emptySet()
        (names - twins).map { "$vietnamese: missing '$it'" } + (twins - names).map { "$english: missing '$it'" }
      }
      .toList()
    assertEquals(emptyList<String>(), problems)
  }

  /** `app_name` is the brand, the same in every language. */
  private fun stringNames(file: File): Set<String> = STRING.findAll(file.readText())
    .filter { "translatable=\"false\"" !in it.value }
    .map { it.groupValues[1] }
    .filter { it != "app_name" }
    .toSet()

  private companion object {
    val VIEW_MODEL = Regex("""@Introspected(?:\([^)]*\))?\s+class (\w+)\(""")
    val STRING = Regex("""<(?:string|plurals|string-array) [^>]*name="([^"]+)"[^>]*>""")
  }
}
