package androidshowcase.reflex.feature.pokedex.ui

import androidshowcase.reflex.designsystem.ErrorState
import androidshowcase.reflex.designsystem.LoadingState
import androidshowcase.reflex.designsystem.ReflexTheme
import androidshowcase.reflex.designsystem.errorMessage
import androidshowcase.reflex.domain.model.PokemonDetail
import androidshowcase.reflex.domain.model.PokemonStat
import androidshowcase.reflex.feature.pokedex.PokemonDetailViewModel
import androidshowcase.reflex.feature.pokedex.PokemonDetailViewModel.Action
import androidshowcase.reflex.feature.pokedex.PokemonDetailViewModel.State
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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

private const val MAX_BASE_STAT = 255f

@Composable
fun PokemonDetailScreen(viewModel: PokemonDetailViewModel, onBack: () -> Unit) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  PokemonDetailContent(state, viewModel::send, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailContent(state: State, onAction: (Action) -> Unit, onBack: () -> Unit) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(state.pokemon?.name?.displayName() ?: state.id.dexNumber()) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.pokedex_back))
          }
        },
        actions = {
          if (state.pokemon != null) {
            IconButton(onClick = { onAction(Action.ToggleFavorite) }, enabled = !state.isSavingFavorite) {
              Icon(
                imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(
                  if (state.isFavorite) R.string.pokedex_unfavorite else R.string.pokedex_favorite,
                ),
              )
            }
          }
        },
      )
    },
  ) { padding ->
    val pokemon = state.pokemon
    val error = state.error
    when {
      pokemon != null -> Details(pokemon, state, Modifier.padding(padding))
      error != null -> ErrorState(error, onRetry = { onAction(Action.Retry) }, Modifier.padding(padding))
      else -> LoadingState(Modifier.padding(padding))
    }
  }
}

@Composable
private fun Details(pokemon: PokemonDetail, state: State, modifier: Modifier) {
  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    AsyncImage(
      model = pokemon.artworkUrl,
      contentDescription = null,
      modifier = Modifier.fillMaxWidth().aspectRatio(1.4f),
    )
    state.favoriteError?.let { Text(errorMessage(it), color = MaterialTheme.colorScheme.error) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      pokemon.types.forEach { type -> SuggestionChip(onClick = {}, label = { Text(type.displayName()) }) }
    }
    Text(
      stringResource(R.string.pokedex_size, pokemon.heightDecimetres / 10f, pokemon.weightHectograms / 10f),
      style = MaterialTheme.typography.bodyLarge,
    )
    pokemon.stats.forEach { StatRow(it) }
  }
}

@Composable
private fun StatRow(stat: PokemonStat) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(statLabel(stat.name), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp))
    Text(stat.value.toString(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(32.dp))
    LinearProgressIndicator(progress = { stat.value / MAX_BASE_STAT }, modifier = Modifier.weight(1f))
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
private fun DetailPreview() = ReflexTheme(dynamicColor = false) {
  val stats = listOf(PokemonStat("hp", 35), PokemonStat("speed", 90))
  val pikachu = PokemonDetail(25, "pikachu", "", "", 4, 60, listOf("electric"), stats)
  val state = State(id = 25, pokemon = pikachu, isLoading = false, isFavorite = true)
  PokemonDetailContent(state, onAction = {}, onBack = {})
}
