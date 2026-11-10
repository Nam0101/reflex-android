package androidshowcase.reflex.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cold start to the Pokédex. The pair of tests shows what the Baseline Profile buys. */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
  @get:Rule
  val rule = MacrobenchmarkRule()

  @Test
  fun coldStartupWithoutProfile() = coldStartup(CompilationMode.None())

  @Test
  fun coldStartupWithBaselineProfile() = coldStartup(CompilationMode.Partial(BaselineProfileMode.Require))

  private fun coldStartup(compilationMode: CompilationMode) = rule.measureRepeated(
    packageName = PACKAGE,
    metrics = listOf(StartupTimingMetric()),
    compilationMode = compilationMode,
    iterations = 10,
    startupMode = StartupMode.COLD,
    setupBlock = {
      // Every measured start lands on the Pokédex, so first launch is walked once, outside the measurement.
      if (iteration == 0) {
        launchToPokedex()
        killProcess()
      }
      pressHome()
    },
  ) {
    startActivityAndWait()
  }
}

/** Frame times while flinging the Pokédex grid, with and without the Baseline Profile. */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {
  @get:Rule
  val rule = MacrobenchmarkRule()

  @Test
  fun scrollPokedexWithoutProfile() = scrollPokedex(CompilationMode.None())

  @Test
  fun scrollPokedexWithBaselineProfile() = scrollPokedex(CompilationMode.Partial(BaselineProfileMode.Require))

  private fun scrollPokedex(compilationMode: CompilationMode) = rule.measureRepeated(
    packageName = PACKAGE,
    metrics = listOf(FrameTimingMetric()),
    compilationMode = compilationMode,
    iterations = 5,
    startupMode = StartupMode.WARM,
    setupBlock = { launchToPokedex() },
  ) {
    flingPokedex()
  }
}

/**
 * Two profiles: the start alone also becomes the startup profile (DEX layout), the whole journey (first launch,
 * scrolling the grid, a detail screen and back) becomes the Baseline Profile.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
  @get:Rule
  val rule = BaselineProfileRule()

  @Test
  fun startup() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
    launchToPokedex()
  }

  @Test
  fun journey() = rule.collect(packageName = PACKAGE) {
    launchToPokedex()
    flingPokedex()
    openFirstPokemonAndGoBack()
  }
}
