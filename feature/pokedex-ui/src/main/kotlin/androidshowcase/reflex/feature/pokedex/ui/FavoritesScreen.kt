package androidshowcase.reflex.feature.pokedex.ui

import androidshowcase.reflex.designsystem.ErrorState
import androidshowcase.reflex.designsystem.LoadingState
import androidshowcase.reflex.designsystem.ReflexTheme
import androidshowcase.reflex.domain.model.PokemonSummary
import androidshowcase.reflex.feature.pokedex.FavoritesViewModel
import androidshowcase.reflex.feature.pokedex.FavoritesViewModel.Action
import androidshowcase.reflex.feature.pokedex.FavoritesViewModel.State
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

@Composable
fun FavoritesScreen(viewModel: FavoritesViewModel) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  FavoritesContent(state, viewModel::send)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesContent(state: State, onAction: (Action) -> Unit) {
  Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.favorites_title)) }) }) { padding ->
    val error = state.error
    when {
      state.isLoading -> LoadingState(Modifier.padding(padding))

      error != null && state.items.isEmpty() -> ErrorState(error, onRetry = {}, Modifier.padding(padding))

      state.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.favorites_empty), style = MaterialTheme.typography.bodyLarge)
      }

      else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
        items(state.items, key = { it.id }, contentType = { "favorite" }) { pokemon ->
          FavoriteRow(pokemon, onAction)
        }
      }
    }
  }
}

@Composable
private fun FavoriteRow(pokemon: PokemonSummary, onAction: (Action) -> Unit) {
  ListItem(
    modifier = Modifier.clickable { onAction(Action.Open(pokemon.id)) },
    leadingContent = {
      AsyncImage(model = pokemon.spriteUrl, contentDescription = null, modifier = Modifier.size(56.dp))
    },
    headlineContent = { Text(pokemon.name.displayName()) },
    supportingContent = { Text(pokemon.id.dexNumber()) },
    trailingContent = {
      IconButton(onClick = { onAction(Action.Remove(pokemon.id)) }) {
        Icon(
          Icons.Filled.Delete,
          contentDescription = stringResource(R.string.favorites_remove, pokemon.name.displayName()),
        )
      }
    },
  )
}

@Preview
@Composable
private fun FavoritesPreview() = ReflexTheme(dynamicColor = false) {
  FavoritesContent(State(items = listOf(PokemonSummary(25, "pikachu", "")), isLoading = false), onAction = {})
}
