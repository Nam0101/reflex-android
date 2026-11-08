package androidshowcase.reflex.feature.onboarding.ui

import androidshowcase.reflex.designsystem.ReflexTheme
import androidshowcase.reflex.designsystem.errorMessage
import androidshowcase.reflex.domain.model.AppLanguage
import androidshowcase.reflex.feature.onboarding.LanguageViewModel
import androidshowcase.reflex.feature.onboarding.LanguageViewModel.Action
import androidshowcase.reflex.feature.onboarding.LanguageViewModel.State
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Binds the view to its ViewModel: reads `state`, calls `send`. Nothing else. */
@Composable
fun LanguageScreen(viewModel: LanguageViewModel) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  LanguageContent(state, viewModel::send)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageContent(state: State, onAction: (Action) -> Unit) {
  Scaffold(
    topBar = { TopAppBar(title = { Text(stringResource(R.string.onboarding_language_title)) }) },
    bottomBar = {
      Column(Modifier.padding(16.dp)) {
        state.error?.let { Text(errorMessage(it), color = MaterialTheme.colorScheme.error) }
        Button(
          onClick = { onAction(Action.Continue) },
          enabled = state.canContinue,
          modifier = Modifier.fillMaxWidth(),
        ) {
          if (state.isSaving) {
            CircularProgressIndicator(Modifier.size(18.dp))
          } else {
            Text(stringResource(R.string.onboarding_continue))
          }
        }
      }
    },
  ) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding).selectableGroup()) {
      items(state.languages, key = { it.tag }) { language ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = language == state.selected, role = Role.RadioButton) {
              onAction(Action.Select(language))
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          RadioButton(selected = language == state.selected, onClick = null)
          Text(language.nativeName, style = MaterialTheme.typography.bodyLarge)
        }
      }
    }
  }
}

@Preview
@Composable
private fun LanguagePreview() = ReflexTheme(dynamicColor = false) {
  LanguageContent(State(selected = AppLanguage.Vietnamese), onAction = {})
}
