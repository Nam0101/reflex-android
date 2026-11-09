package androidshowcase.reflex.feature.pokedex.ui

import androidshowcase.reflex.designsystem.ErrorState
import androidshowcase.reflex.designsystem.LargeHeader
import androidshowcase.reflex.designsystem.PokeballMark
import androidshowcase.reflex.designsystem.ReflexTheme
import androidshowcase.reflex.designsystem.errorMessage
import androidshowcase.reflex.designsystem.sharedBackground
import androidshowcase.reflex.domain.error.AppError
import androidshowcase.reflex.domain.model.PokemonSummary
import androidshowcase.reflex.feature.pokedex.PokedexViewModel
import androidshowcase.reflex.feature.pokedex.PokedexViewModel.Action
import androidshowcase.reflex.feature.pokedex.PokedexViewModel.State
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val PREFETCH_DISTANCE = 6
private const val PLACEHOLDERS = 6

@Composable
fun PokedexScreen(viewModel: PokedexViewModel) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  PokedexContent(state, viewModel::send)
}

@Composable
fun PokedexContent(state: State, onAction: (Action) -> Unit) {
  val gridState = rememberLazyGridState()
  LoadMoreNearEnd(gridState, onLoadMore = { onAction(Action.LoadMore) })
  val error = state.error
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 156.dp),
    state = gridState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
      LargeHeader(stringResource(R.string.pokedex_title), stringResource(R.string.pokedex_subtitle))
    }
    when {
      state.items.isEmpty() && error != null -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "error") {
        ErrorState(error, onRetry = { onAction(Action.Retry) }, Modifier.height(360.dp))
      }

      state.items.isEmpty() -> items(PLACEHOLDERS, contentType = { "placeholder" }) { PlaceholderCard() }

      else -> {
        items(state.items, key = { it.id }, contentType = { "pokemon" }) { pokemon ->
          PokemonCard(pokemon, onClick = { onAction(Action.Open(pokemon.id)) })
        }
        if (!state.endReached) {
          item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
            Footer(error, onRetry = { onAction(Action.Retry) })
          }
        }
      }
    }
  }
}

/** Sends LoadMore when the last visible item gets close to the end. The ViewModel drops duplicates. */
@Composable
private fun LoadMoreNearEnd(gridState: LazyGridState, onLoadMore: () -> Unit) {
  val currentOnLoadMore by rememberUpdatedState(onLoadMore)
  LaunchedEffect(gridState) {
    snapshotFlow {
      val info = gridState.layoutInfo
      val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
      info.totalItemsCount > 0 && last >= info.totalItemsCount - PREFETCH_DISTANCE
    }.distinctUntilChanged().filter { it }.collect { currentOnLoadMore() }
  }
}

@Composable
private fun PokemonCard(pokemon: PokemonSummary, onClick: () -> Unit) {
  val color = rememberArtworkColor(pokemon.id)
  val (container, content) = pokemonCardColors(color.value)
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().aspectRatio(0.8f),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = content),
  ) {
    Box(Modifier.fillMaxSize()) {
      // Its own layer, so it can morph into the detail header's background.
      Box(
        Modifier
          .matchParentSize()
          .sharedBackground(backgroundKey(pokemon.id))
          .background(container, MaterialTheme.shapes.large),
      )
      PokeballMark(
        Modifier.size(132.dp).align(Alignment.BottomEnd).offset(x = 34.dp, y = 34.dp).rotate(-20f),
        color = content.copy(alpha = 0.16f),
      )
      Column(Modifier.fillMaxSize().padding(14.dp)) {
        Text(
          pokemon.id.dexNumber(),
          style = MaterialTheme.typography.labelLarge,
          color = content.copy(alpha = 0.8f),
          modifier = Modifier.align(Alignment.End),
        )
        PokemonArtwork(
          pokemon.id,
          pokemon.artworkUrl,
          color,
          Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
        )
        Text(
          pokemon.name.displayName(),
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun PlaceholderCard() {
  val transition = rememberInfiniteTransition(label = "placeholder")
  val alpha by transition.animateFloat(0.45f, 0.9f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha")
  Box(
    Modifier
      .fillMaxWidth()
      .aspectRatio(0.8f)
      .graphicsLayer { this.alpha = alpha }
      .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large),
  )
}

@Composable
private fun Footer(error: AppError?, onRetry: () -> Unit) {
  Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
    if (error != null) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(errorMessage(error), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.pokedex_retry)) }
      }
    } else {
      CircularProgressIndicator(strokeCap = StrokeCap.Round)
    }
  }
}

@Preview
@Composable
private fun PokedexPreview() = ReflexTheme {
  val items = listOf(PokemonSummary(1, "bulbasaur", "", ""), PokemonSummary(4, "charmander", "", ""))
  PokedexContent(State(items = items, isLoading = false), onAction = {})
}
