package androidshowcase.reflex.feature.pokedex.ui

import androidshowcase.reflex.designsystem.ErrorState
import androidshowcase.reflex.designsystem.LoadingState
import androidshowcase.reflex.designsystem.ReflexTheme
import androidshowcase.reflex.designsystem.errorMessage
import androidshowcase.reflex.domain.model.PokemonSummary
import androidshowcase.reflex.feature.pokedex.PokedexViewModel
import androidshowcase.reflex.feature.pokedex.PokedexViewModel.Action
import androidshowcase.reflex.feature.pokedex.PokedexViewModel.State
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val PREFETCH_DISTANCE = 6

@Composable
fun PokedexScreen(viewModel: PokedexViewModel) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  PokedexContent(state, viewModel::send)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexContent(state: State, onAction: (Action) -> Unit) {
  Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.pokedex_title)) }) }) { padding ->
    val error = state.error
    when {
      state.items.isEmpty() && error != null ->
        ErrorState(error, onRetry = { onAction(Action.Retry) }, Modifier.padding(padding))

      state.items.isEmpty() -> LoadingState(Modifier.padding(padding))

      else -> PokemonGrid(state, onAction, PaddingValues(16.dp), Modifier.padding(padding))
    }
  }
}

@Composable
private fun PokemonGrid(state: State, onAction: (Action) -> Unit, contentPadding: PaddingValues, modifier: Modifier) {
  val gridState = rememberLazyGridState()
  LoadMoreNearEnd(gridState, onLoadMore = { onAction(Action.LoadMore) })
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 112.dp),
    state = gridState,
    modifier = modifier.fillMaxSize(),
    contentPadding = contentPadding,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(state.items, key = { it.id }, contentType = { "pokemon" }) { pokemon ->
      PokemonCard(pokemon, onClick = { onAction(Action.Open(pokemon.id)) })
    }
    if (!state.endReached) {
      item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
          val error = state.error
          if (error != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(errorMessage(error), color = MaterialTheme.colorScheme.error)
              TextButton(onClick = { onAction(Action.Retry) }) { Text(stringResource(R.string.pokedex_retry)) }
            }
          } else {
            CircularProgressIndicator()
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
  ElevatedCard(Modifier.fillMaxWidth().clickable(onClickLabel = pokemon.name.displayName(), onClick = onClick)) {
    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      AsyncImage(
        model = pokemon.spriteUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
      )
      Text(
        pokemon.id.dexNumber(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(pokemon.name.displayName(), style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
  }
}

@Preview
@Composable
private fun PokedexPreview() = ReflexTheme(dynamicColor = false) {
  PokedexContent(
    State(items = listOf(PokemonSummary(1, "bulbasaur", ""), PokemonSummary(4, "charmander", "")), isLoading = false),
    onAction = {},
  )
}
