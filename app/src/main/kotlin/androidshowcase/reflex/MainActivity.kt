package androidshowcase.reflex

import android.content.Intent
import android.os.Bundle
import androidshowcase.reflex.designsystem.ReflexTheme
import androidshowcase.reflex.ui.ReflexHost
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val app = reflexApp
    // The system splash stays up while Core decides the first screen (reads settings from disk).
    splashScreen.setKeepOnScreenCondition { app.isLaunching }
    if (savedInstanceState == null) {
      openDeepLink(intent)
    } else {
      // After process death Core starts over: hand it the screens the user had.
      savedInstanceState.getString(NAVIGATION_STATE)?.let(app::restoreNavigation)
    }
    setContent {
      ReflexTheme { ReflexHost(app.navigator) }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    reflexApp.saveNavigation()?.let { outState.putString(NAVIGATION_STATE, it) }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    openDeepLink(intent)
  }

  private fun openDeepLink(intent: Intent?) {
    intent?.dataString?.let(reflexApp::openDeepLink)
  }

  private companion object {
    const val NAVIGATION_STATE = "reflex.navigation"
  }
}
