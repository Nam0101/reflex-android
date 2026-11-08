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
    if (savedInstanceState == null) openDeepLink(intent)
    setContent {
      ReflexTheme { ReflexHost(app.navigator) }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    openDeepLink(intent)
  }

  private fun openDeepLink(intent: Intent?) {
    intent?.dataString?.let(reflexApp::openDeepLink)
  }
}
