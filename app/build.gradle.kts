import java.util.Properties

// Mobile shell: Activity, Compose host, platform services. Views render Core state and send actions.
plugins {
  alias(libs.plugins.reflex.android.application)
  alias(libs.plugins.reflex.compose)
  alias(libs.plugins.baselineprofile)
}

// Release signing comes from keystore.properties (never committed). Without it, release builds are
// signed with the debug key so they can be installed for testing; Play rejects such uploads.
val keystore = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
  Properties().apply { file.inputStream().use(::load) }
}

android {
  namespace = "androidshowcase.reflex"

  defaultConfig {
    applicationId = "androidshowcase.reflex"
    versionCode = 2
    versionName = "1.1.0"
  }

  signingConfigs {
    if (keystore != null) {
      create("release") {
        storeFile = rootProject.file(keystore.getProperty("storeFile"))
        storePassword = keystore.getProperty("storePassword")
        keyAlias = keystore.getProperty("keyAlias")
        keyPassword = keystore.getProperty("keyPassword")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  buildFeatures {
    buildConfig = true
  }

  androidResources {
    generateLocaleConfig = true
  }
}

dependencies {
  implementation(projects.shell)
  implementation(projects.core.designsystem)
  implementation(projects.feature.onboardingUi)
  implementation(projects.feature.pokedexUi)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.profileinstaller)
  implementation(libs.kotlinx.coroutines.android)

  baselineProfile(projects.benchmark)
}
