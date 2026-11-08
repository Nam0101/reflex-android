package androidshowcase.reflex.benchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "androidshowcase.reflex"

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
  @get:Rule
  val rule = MacrobenchmarkRule()

  @Test
  fun coldStartup() = rule.measureRepeated(
    packageName = PACKAGE,
    metrics = listOf(StartupTimingMetric()),
    iterations = 5,
    startupMode = StartupMode.COLD,
  ) {
    pressHome()
    startActivityAndWait()
  }
}

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
  @get:Rule
  val rule = BaselineProfileRule()

  @Test
  fun generate() = rule.collect(packageName = PACKAGE) {
    pressHome()
    startActivityAndWait()
  }
}
