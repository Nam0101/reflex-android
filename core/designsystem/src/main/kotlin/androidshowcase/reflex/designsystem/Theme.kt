package androidshowcase.reflex.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Pokédex red on warm neutrals. Containers lean red so selection indicators match the brand.
private val LightColors = lightColorScheme(
  primary = Color(0xFFCF3010),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFFDAD2),
  onPrimaryContainer = Color(0xFF3E0500),
  secondary = Color(0xFF775750),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFFFE2DC),
  onSecondaryContainer = Color(0xFF2C1511),
  tertiary = Color(0xFF6E5D0E),
  tertiaryContainer = Color(0xFFF9E287),
  background = Color(0xFFFCF8F7),
  onBackground = Color(0xFF1E1B1A),
  surface = Color(0xFFFCF8F7),
  onSurface = Color(0xFF1E1B1A),
  surfaceVariant = Color(0xFFF4DDD9),
  onSurfaceVariant = Color(0xFF5B4F4C),
  surfaceContainerLowest = Color.White,
  surfaceContainerLow = Color(0xFFF7F2F1),
  surfaceContainer = Color(0xFFF2ECEB),
  surfaceContainerHigh = Color(0xFFECE6E5),
  surfaceContainerHighest = Color(0xFFE6E1E0),
  outline = Color(0xFF857371),
  outlineVariant = Color(0xFFD8C2BE),
)

private val DarkColors = darkColorScheme(
  primary = Color(0xFFFFB4A3),
  onPrimary = Color(0xFF5F1300),
  primaryContainer = Color(0xFF861F02),
  onPrimaryContainer = Color(0xFFFFDAD2),
  secondary = Color(0xFFE7BDB5),
  onSecondary = Color(0xFF442A24),
  secondaryContainer = Color(0xFF5D3F39),
  onSecondaryContainer = Color(0xFFFFDAD3),
  tertiary = Color(0xFFDCC66E),
  tertiaryContainer = Color(0xFF544600),
  background = Color(0xFF141110),
  onBackground = Color(0xFFEDE0DD),
  surface = Color(0xFF141110),
  onSurface = Color(0xFFEDE0DD),
  surfaceVariant = Color(0xFF534341),
  onSurfaceVariant = Color(0xFFD8C2BE),
  surfaceContainerLowest = Color(0xFF0F0C0B),
  surfaceContainerLow = Color(0xFF1C1918),
  surfaceContainer = Color(0xFF211D1C),
  surfaceContainerHigh = Color(0xFF2B2726),
  surfaceContainerHighest = Color(0xFF363231),
  outline = Color(0xFFA08C89),
  outlineVariant = Color(0xFF534341),
)

// Be Vietnam Pro: geometric, friendly, and drawn for Vietnamese diacritics (SIL OFL, see FONT-LICENSE-OFL.txt).
private val BeVietnamPro = FontFamily(
  Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
  Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
  Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
  Font(R.font.be_vietnam_pro_extrabold, FontWeight.ExtraBold),
)

private fun TextStyle.brand(weight: FontWeight, tracking: Double? = null) =
  copy(fontFamily = BeVietnamPro, fontWeight = weight, letterSpacing = tracking?.sp ?: letterSpacing)

private val ReflexTypography = Typography().run {
  copy(
    displayLarge = displayLarge.brand(FontWeight.ExtraBold, -1.0),
    displayMedium = displayMedium.brand(FontWeight.ExtraBold, -0.8),
    displaySmall = displaySmall.brand(FontWeight.ExtraBold, -0.6),
    headlineLarge = headlineLarge.brand(FontWeight.ExtraBold, -0.5),
    headlineMedium = headlineMedium.brand(FontWeight.Bold, -0.3),
    headlineSmall = headlineSmall.brand(FontWeight.Bold),
    titleLarge = titleLarge.brand(FontWeight.Bold),
    titleMedium = titleMedium.brand(FontWeight.SemiBold),
    titleSmall = titleSmall.brand(FontWeight.SemiBold),
    bodyLarge = bodyLarge.brand(FontWeight.Normal),
    bodyMedium = bodyMedium.brand(FontWeight.Normal),
    bodySmall = bodySmall.brand(FontWeight.Normal),
    labelLarge = labelLarge.brand(FontWeight.SemiBold),
    labelMedium = labelMedium.brand(FontWeight.SemiBold),
    labelSmall = labelSmall.brand(FontWeight.SemiBold),
  )
}

private val ReflexShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(26.dp),
  extraLarge = RoundedCornerShape(32.dp),
)

/** Material 3 theme with the Reflex brand. Pass `dynamicColor = true` to follow the wallpaper on Android 12+. */
@Composable
fun ReflexTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
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
  MaterialTheme(colorScheme = colorScheme, typography = ReflexTypography, shapes = ReflexShapes, content = content)
}
