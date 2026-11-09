package androidshowcase.reflex.feature.pokedex.ui

import androidshowcase.reflex.designsystem.ErrorState
import androidshowcase.reflex.designsystem.LargeHeader
import androidshowcase.reflex.designsystem.LoadingState
import androidshowcase.reflex.designsystem.PokeballMark
import androidshowcase.reflex.designsystem.ReflexTheme
import androidshowcase.reflex.designsystem.sharedBackground
import androidshowcase.reflex.domain.model.PokemonSummary
import androidshowcase.reflex.feature.pokedex.FavoritesViewModel
import androidshowcase.reflex.feature.pokedex.FavoritesViewModel.Action
import androidshowcase.reflex.feature.pokedex.FavoritesViewModel.State
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FavoritesScreen(viewModel: FavoritesViewModel) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  FavoritesContent(state, viewModel::send)
}

@Composable
fun FavoritesContent(state: State, onAction: (Action) -> Unit) {
  val error = state.error
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(contentType = "header") {
      val count = state.items.size
      LargeHeader(
        stringResource(R.string.favorites_title),
        subtitle = if (count == 0) null else pluralStringResource(R.plurals.favorites_count, count, count),
      )
    }
    when {
      state.isLoading -> item(contentType = "loading") { LoadingState(Modifier.height(320.dp)) }

      error != null && state.items.isEmpty() -> item(contentType = "error") {
        ErrorState(error, onRetry = { onAction(Action.Retry) }, Modifier.height(320.dp))
      }

      state.items.isEmpty() -> item(contentType = "empty") { EmptyFavorites() }

      else -> items(state.items, key = { it.id }, contentType = { "favorite" }) { pokemon ->
        FavoriteCard(pokemon, onAction, Modifier.animateItem())
      }
    }
  }
}

@Composable
private fun FavoriteCard(pokemon: PokemonSummary, onAction: (Action) -> Unit, modifier: Modifier) {
  val color = rememberArtworkColor(pokemon.id)
  val (container, content) = pokemonCardColors(color.value)
  Card(
    onClick = { onAction(Action.Open(pokemon.id)) },
    modifier = modifier.fillMaxWidth().height(104.dp),
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
        Modifier.size(150.dp).align(Alignment.CenterEnd).offset(x = 44.dp).rotate(-20f),
        color = content.copy(alpha = 0.14f),
      )
      Row(Modifier.fillMaxSize().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        PokemonArtwork(pokemon.id, pokemon.artworkUrl, color, Modifier.size(84.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
          Text(pokemon.id.dexNumber(), style = MaterialTheme.typography.labelLarge, color = content.copy(alpha = 0.8f))
          Text(
            pokemon.name.displayName(),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(onClick = { onAction(Action.Remove(pokemon.id)) }) {
          Icon(
            Icons.Filled.Delete,
            contentDescription = stringResource(R.string.favorites_remove, pokemon.name.displayName()),
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyFavorites() {
  Column(
    Modifier.fillMaxWidth().padding(top = 72.dp, start = 24.dp, end = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    PokeballMark(Modifier.size(112.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
    Text(stringResource(R.string.favorites_empty_title), style = MaterialTheme.typography.titleLarge)
    Text(
      stringResource(R.string.favorites_empty),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Preview
@Composable
private fun FavoritesPreview() = ReflexTheme {
  FavoritesContent(State(items = listOf(PokemonSummary(25, "pikachu", "", "")), isLoading = false), onAction = {})
}
