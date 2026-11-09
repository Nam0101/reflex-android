package androidshowcase.reflex.feature.pokedex.ui

import androidshowcase.reflex.designsystem.ErrorState
import androidshowcase.reflex.designsystem.LoadingState
import androidshowcase.reflex.designsystem.PokeballMark
import androidshowcase.reflex.designsystem.ReflexTheme
import androidshowcase.reflex.designsystem.errorMessage
import androidshowcase.reflex.designsystem.sharedBackground
import androidshowcase.reflex.designsystem.sharedElement
import androidshowcase.reflex.domain.model.PokemonDetail
import androidshowcase.reflex.domain.model.PokemonStat
import androidshowcase.reflex.feature.pokedex.PokemonDetailViewModel
import androidshowcase.reflex.feature.pokedex.PokemonDetailViewModel.Action
import androidshowcase.reflex.feature.pokedex.PokemonDetailViewModel.State
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

// Bars are drawn against 200 so typical base stats (40-130) spread across the track; higher values fill it.
private const val STAT_SCALE = 200f

@Composable
fun PokemonDetailScreen(viewModel: PokemonDetailViewModel, onBack: () -> Unit) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  PokemonDetailContent(state, viewModel::send, onBack)
}

@Composable
fun PokemonDetailContent(state: State, onAction: (Action) -> Unit, onBack: () -> Unit) {
  val scheme = MaterialTheme.colorScheme
  val typeColor = state.pokemon?.types?.firstOrNull()?.let(::typeColor)
  // Until the detail loads, the artwork's color (decoded by the list card) seeds the header, so the card's
  // background morphs into a header of the same color instead of a grey one.
  val artworkColor by rememberArtworkColor(state.id)
  val seed = typeColor ?: artworkColor
  // Darkened a touch in light theme so most types keep white text, more in dark theme.
  val headerTarget = when {
    seed == null -> scheme.surfaceContainerHighest
    scheme.surface.luminance() < 0.5f -> lerp(seed, Color.Black, 0.3f)
    else -> lerp(seed, Color.Black, 0.12f)
  }
  val headerColor by animateColorAsState(headerTarget, tween(500), label = "headerColor")
  // Surface provides the content color the body's text inherits (dark mode included).
  Surface(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
      // Drawn above the body so the artwork can overlap the sheet's top edge.
      Header(state, headerColor, contentColorOn(headerTarget), onAction, onBack, Modifier.zIndex(1f))
      val pokemon = state.pokemon
      val error = state.error
      when {
        pokemon != null -> Body(pokemon, state, statColor = typeColor ?: scheme.primary)
        error != null -> ErrorState(error, onRetry = { onAction(Action.Retry) }, Modifier.height(320.dp))
        else -> LoadingState(Modifier.height(320.dp))
      }
    }
  }
}

@Composable
private fun Header(
  state: State,
  color: Color,
  contentColor: Color,
  onAction: (Action) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier,
) {
  val surface = MaterialTheme.colorScheme.surface
  val pokemon = state.pokemon
  Box(modifier.fillMaxWidth()) {
    // Its own layer: the list card's background morphs into it.
    Box(
      Modifier.matchParentSize().sharedBackground(backgroundKey(state.id)).drawWithCache {
        val gradient = Brush.verticalGradient(listOf(color, lerp(color, Color.Black, 0.18f)))
        val lip = 32.dp.toPx()
        val sheetTop = Path().apply {
          addRoundRect(
            RoundRect(
              left = 0f,
              top = size.height - lip,
              right = size.width,
              bottom = size.height,
              topLeftCornerRadius = CornerRadius(lip),
              topRightCornerRadius = CornerRadius(lip),
            ),
          )
        }
        onDrawBehind {
          drawRect(gradient)
          drawPath(sheetTop, surface)
        }
      },
    )
    PokeballMark(
      Modifier.size(300.dp).align(Alignment.TopEnd).offset(x = 90.dp, y = 36.dp).rotate(-20f),
      color = contentColor.copy(alpha = 0.14f),
    )
    CompositionLocalProvider(LocalContentColor provides contentColor) {
      Column(Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.pokedex_back))
          }
          Spacer(Modifier.weight(1f))
          if (pokemon != null) {
            FavoriteButton(state.isFavorite, enabled = !state.isSavingFavorite) { onAction(Action.ToggleFavorite) }
          }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.Bottom) {
          Text(
            pokemon?.name?.displayName().orEmpty(),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f),
          )
          Text(
            state.id.dexNumber(),
            style = MaterialTheme.typography.titleLarge,
            color = contentColor.copy(alpha = 0.75f),
          )
        }
        Row(
          Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          pokemon?.types?.forEach { TypeChip(it, contentColor) }
        }
        Box(Modifier.fillMaxWidth().height(196.dp)) {
          // Shown before the detail loads: it is the element the list's card hands over.
          AsyncImage(
            model = state.artworkUrl,
            contentDescription = pokemon?.name?.displayName(),
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .offset(y = 56.dp)
              .sharedElement(artworkKey(state.id))
              .size(256.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, enabled: Boolean, onClick: () -> Unit) {
  val scale by animateFloatAsState(
    if (isFavorite) 1.2f else 1f,
    spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
    label = "heart",
  )
  IconButton(onClick = onClick, enabled = enabled) {
    Icon(
      imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
      contentDescription = stringResource(if (isFavorite) R.string.pokedex_unfavorite else R.string.pokedex_favorite),
      modifier = Modifier.scale(scale),
    )
  }
}

@Composable
private fun TypeChip(type: String, contentColor: Color) {
  Text(
    type.displayName(),
    style = MaterialTheme.typography.labelLarge,
    modifier = Modifier
      .background(contentColor.copy(alpha = 0.2f), CircleShape)
      .padding(horizontal = 14.dp, vertical = 6.dp),
  )
}

@Composable
private fun Body(pokemon: PokemonDetail, state: State, statColor: Color) {
  // The top padding leaves room for the artwork hanging over from the header.
  Column(
    Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(start = 24.dp, end = 24.dp, top = 76.dp, bottom = 32.dp),
  ) {
    state.favoriteError?.let {
      Text(errorMessage(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Fact(
        stringResource(R.string.pokedex_height),
        stringResource(R.string.pokedex_height_value, pokemon.heightDecimetres / 10f),
        Modifier.weight(1f),
      )
      Fact(
        stringResource(R.string.pokedex_weight),
        stringResource(R.string.pokedex_weight_value, pokemon.weightHectograms / 10f),
        Modifier.weight(1f),
      )
    }
    Text(
      stringResource(R.string.pokedex_base_stats),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(top = 28.dp, bottom = 8.dp),
    )
    pokemon.stats.forEachIndexed { index, stat -> StatRow(stat, statColor, index) }
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(
        stringResource(R.string.pokedex_total),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.width(112.dp),
      )
      Text(pokemon.stats.sumOf { it.value }.toString(), style = MaterialTheme.typography.titleMedium)
    }
  }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier) {
  val container = MaterialTheme.colorScheme.surfaceContainerLow
  Column(modifier.background(container, MaterialTheme.shapes.medium).padding(16.dp)) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.titleLarge)
  }
}

@Composable
private fun StatRow(stat: PokemonStat, color: Color, index: Int) {
  var shown by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { shown = true }
  val progress = animateFloatAsState(
    if (shown) (stat.value / STAT_SCALE).coerceAtMost(1f) else 0f,
    tween(durationMillis = 800, delayMillis = 70 * index, easing = FastOutSlowInEasing),
    label = "stat",
  )
  Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(
      statLabel(stat.name),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.width(112.dp),
    )
    Text(stat.value.toString(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(44.dp))
    // Reads the animated value while drawing only: the bar animates without recomposing the row.
    Box(
      Modifier.weight(1f).height(8.dp).drawBehind {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color.copy(alpha = 0.16f), cornerRadius = radius)
        drawRoundRect(color, size = Size(size.width * progress.value, size.height), cornerRadius = radius)
      },
    )
  }
}

@Composable
private fun statLabel(name: String): String = when (name) {
  "hp" -> stringResource(R.string.pokedex_stat_hp)
  "attack" -> stringResource(R.string.pokedex_stat_attack)
  "defense" -> stringResource(R.string.pokedex_stat_defense)
  "special-attack" -> stringResource(R.string.pokedex_stat_special_attack)
  "special-defense" -> stringResource(R.string.pokedex_stat_special_defense)
  "speed" -> stringResource(R.string.pokedex_stat_speed)
  else -> name.displayName()
}

@Preview
@Composable
private fun DetailPreview() = ReflexTheme {
  val stats = listOf(PokemonStat("hp", 35), PokemonStat("attack", 55), PokemonStat("speed", 90))
  val pikachu = PokemonDetail(25, "pikachu", "", "", 4, 60, listOf("electric"), stats)
  val state = State(id = 25, artworkUrl = "", pokemon = pikachu, isLoading = false, isFavorite = true)
  PokemonDetailContent(state, onAction = {}, onBack = {})
}
