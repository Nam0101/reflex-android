package androidshowcase.reflex.feature.onboarding.ui

import androidshowcase.reflex.designsystem.PokeballMark
import androidshowcase.reflex.designsystem.ReflexTheme
import androidshowcase.reflex.designsystem.errorMessage
import androidshowcase.reflex.domain.model.AppLanguage
import androidshowcase.reflex.feature.onboarding.LanguageViewModel
import androidshowcase.reflex.feature.onboarding.LanguageViewModel.Action
import androidshowcase.reflex.feature.onboarding.LanguageViewModel.State
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
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

@Composable
fun LanguageContent(state: State, onAction: (Action) -> Unit) {
  Scaffold(
    bottomBar = {
      Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        state.error?.let {
          Text(errorMessage(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }
        Button(
          onClick = { onAction(Action.Continue) },
          enabled = state.canContinue,
          modifier = Modifier.fillMaxWidth().height(56.dp),
          shape = MaterialTheme.shapes.large,
        ) {
          if (state.isSaving) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, strokeCap = StrokeCap.Round)
          } else {
            Text(stringResource(R.string.onboarding_continue), style = MaterialTheme.typography.titleMedium)
          }
        }
      }
    },
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding).selectableGroup(),
      contentPadding = PaddingValues(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(contentType = "intro") { Intro() }
      items(state.languages, key = { it.tag }, contentType = { "language" }) { language ->
        LanguageOption(language, selected = language == state.selected) { onAction(Action.Select(language)) }
      }
    }
  }
}

@Composable
private fun Intro() {
  val colors = MaterialTheme.colorScheme
  Column(Modifier.padding(top = 24.dp, bottom = 20.dp)) {
    Box(Modifier.size(72.dp).background(colors.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
      PokeballMark(Modifier.size(40.dp), color = colors.primary)
    }
    Text(
      stringResource(R.string.onboarding_welcome),
      style = MaterialTheme.typography.headlineLarge,
      modifier = Modifier.padding(top = 24.dp),
    )
    Text(
      stringResource(R.string.onboarding_language_title),
      style = MaterialTheme.typography.bodyLarge,
      color = colors.onSurfaceVariant,
      modifier = Modifier.padding(top = 8.dp),
    )
  }
}

@Composable
private fun LanguageOption(language: AppLanguage, selected: Boolean, onSelect: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  val shape = MaterialTheme.shapes.large
  val container by animateColorAsState(if (selected) colors.primaryContainer else colors.surfaceContainerLow)
  val border by animateColorAsState(if (selected) colors.primary else colors.outlineVariant)
  Surface(
    // Clipped first so the ripple follows the rounded shape.
    modifier = Modifier.fillMaxWidth().clip(shape).selectable(selected, role = Role.RadioButton, onClick = onSelect),
    shape = shape,
    color = container,
    border = BorderStroke(if (selected) 2.dp else 1.dp, border),
  ) {
    Row(
      Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(language.flag(), style = MaterialTheme.typography.headlineSmall)
      Text(language.nativeName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
      RadioButton(selected = selected, onClick = null)
    }
  }
}

// Exhaustive on purpose: a new AppLanguage fails to compile until it has a flag.
private fun AppLanguage.flag(): String = when (this) {
  AppLanguage.English -> "🇬🇧"
  AppLanguage.Vietnamese -> "🇻🇳"
}

@Preview
@Composable
private fun LanguagePreview() = ReflexTheme {
  LanguageContent(State(selected = AppLanguage.Vietnamese), onAction = {})
}
