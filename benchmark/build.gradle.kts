// Startup and scroll benchmarks and the Baseline Profile generator for :app. They run on a connected device:
//   ./gradlew :app:generateReleaseBaselineProfile     ./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest
// The profile lands in app/src/release/generated/baselineProfiles and is committed.
plugins {
  alias(libs.plugins.android.test)
  alias(libs.plugins.baselineprofile)
}

android {
  namespace = "androidshowcase.reflex.benchmark"
  compileSdk = 37

  defaultConfig {
    minSdk = 28
    targetSdk = 37
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  targetProjectPath = ":app"
}

baselineProfile {
  useConnectedDevices = true
}

dependencies {
  implementation(libs.androidx.test.junit)
  implementation(libs.androidx.uiautomator)
  implementation(libs.androidx.benchmark.macro.junit4)
}
