package androidshowcase.reflex.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
  primary = Color(0xFFB3261E),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFFDAD5),
  onPrimaryContainer = Color(0xFF410001),
  secondary = Color(0xFF775651),
  tertiary = Color(0xFF705C2E),
)

private val DarkColors = darkColorScheme(
  primary = Color(0xFFFFB4A9),
  onPrimary = Color(0xFF690002),
  primaryContainer = Color(0xFF930006),
  onPrimaryContainer = Color(0xFFFFDAD5),
  secondary = Color(0xFFE7BDB6),
  tertiary = Color(0xFFDFC38C),
)

/** Material 3 theme. Dynamic color on Android 12+, brand colors elsewhere. */
@Composable
fun ReflexTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    darkTheme -> DarkColors

    else -> LightColors
  }
  MaterialTheme(colorScheme = colorScheme, content = content)
}
