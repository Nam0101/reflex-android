package androidshowcase.reflex.feature.pokedex.ui

import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.palette.graphics.Palette
import coil3.Image
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val typeColors = mapOf(
  "normal" to Color(0xFFA8A77A), "fire" to Color(0xFFEE8130), "water" to Color(0xFF6390F0),
  "electric" to Color(0xFFF7D02C), "grass" to Color(0xFF7AC74C), "ice" to Color(0xFF96D9D6),
  "fighting" to Color(0xFFC22E28), "poison" to Color(0xFFA33EA1), "ground" to Color(0xFFE2BF65),
  "flying" to Color(0xFFA98FF3), "psychic" to Color(0xFFF95587), "bug" to Color(0xFFA6B91A),
  "rock" to Color(0xFFB6A136), "ghost" to Color(0xFF735797), "dragon" to Color(0xFF6F35FC),
  "dark" to Color(0xFF705746), "steel" to Color(0xFFB7B7CE), "fairy" to Color(0xFFD685AD),
)

/** The classic color of a Pokémon type (`"fire"` → orange). */
internal fun typeColor(type: String): Color = typeColors[type] ?: Color(0xFFA8A77A)

/**
 * Text and icon color that reads on [background]. Below 0.3 relative luminance white text has at least 3:1
 * contrast (WCAG large text), above it the near-black does.
 */
internal fun contentColorOn(background: Color): Color =
  if (background.luminance() > 0.3f) Color(0xFF1E1B1A) else Color.White

// Palette colors per Pokémon id, shared by the grid and the favorites list.
private val artworkColors = LruCache<Int, Int>(512)

/** The artwork's dominant color, cached per Pokémon; null until its image has been decoded once. */
@Composable
internal fun rememberArtworkColor(id: Int): MutableState<Color?> =
  remember(id) { mutableStateOf(artworkColors.get(id)?.let(::Color)) }

/**
 * Official artwork that also extracts its Palette color into [color], off the main thread and once per Pokémon.
 * Palette reads pixels, so these requests decode to software bitmaps.
 */
@Composable
internal fun PokemonArtwork(id: Int, url: String, color: MutableState<Color?>, modifier: Modifier = Modifier) {
  val context = LocalPlatformContext.current
  val request = remember(url) { ImageRequest.Builder(context).data(url).allowHardware(false).crossfade(true).build() }
  var decoded by remember(url) { mutableStateOf<Image?>(null) }
  AsyncImage(model = request, contentDescription = null, modifier = modifier, onSuccess = { decoded = it.result.image })
  val image = decoded
  if (image != null && color.value == null) {
    LaunchedEffect(image) { color.value = artworkColor(id, image) }
  }
}

private suspend fun artworkColor(id: Int, image: Image): Color? = withContext(Dispatchers.Default) {
  val palette = Palette.from(image.toBitmap()).maximumColorCount(16).generate()
  // Dominant is the body color; vibrant tends to pick small accents (eyes, flames).
  val swatch = palette.dominantSwatch ?: palette.vibrantSwatch
  swatch?.rgb?.also { artworkColors.put(id, it) }?.let(::Color)
}

/** Card container and content colors from a Pokémon color: as is in light theme, deep in dark theme. */
@Composable
internal fun pokemonCardColors(seed: Color?): Pair<Color, Color> {
  val scheme = MaterialTheme.colorScheme
  val target = when {
    seed == null -> scheme.surfaceContainerHigh
    scheme.surface.luminance() < 0.5f -> lerp(seed, Color.Black, 0.45f)
    else -> seed
  }
  val container by animateColorAsState(target, tween(400), label = "cardColor")
  return container to contentColorOn(target)
}
