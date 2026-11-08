package androidshowcase.reflex.feature.onboarding.ui

import androidshowcase.reflex.designsystem.ReflexTheme
import androidshowcase.reflex.designsystem.errorMessage
import androidshowcase.reflex.feature.onboarding.OnboardingViewModel
import androidshowcase.reflex.feature.onboarding.OnboardingViewModel.Action
import androidshowcase.reflex.feature.onboarding.OnboardingViewModel.State
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private class Page(val icon: ImageVector, val title: Int, val body: Int)

private val pages = listOf(
  Page(Icons.Filled.Search, R.string.onboarding_page1_title, R.string.onboarding_page1_body),
  Page(Icons.Filled.Favorite, R.string.onboarding_page2_title, R.string.onboarding_page2_body),
  Page(Icons.Filled.Star, R.string.onboarding_page3_title, R.string.onboarding_page3_body),
)

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  OnboardingContent(state, viewModel::send)
}

@Composable
fun OnboardingContent(state: State, onAction: (Action) -> Unit) {
  val pagerState = rememberPagerState(initialPage = state.page) { state.pageCount }
  val currentOnAction by rememberUpdatedState(onAction)

  // The ViewModel owns the page. Follow it, and report swipes back as actions.
  LaunchedEffect(state.page) {
    if (pagerState.currentPage != state.page) pagerState.animateScrollToPage(state.page)
  }
  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.settledPage }.collect { currentOnAction(Action.ShowPage(it)) }
  }

  Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
      val page = pages[index % pages.size]
      Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
          page.icon,
          contentDescription = null,
          modifier = Modifier.size(96.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(page.title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(stringResource(page.body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
      }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
      val colors = MaterialTheme.colorScheme
      repeat(state.pageCount) { index ->
        val color = if (index == state.page) colors.primary else colors.outlineVariant
        Box(Modifier.padding(4.dp).size(8.dp).background(color, CircleShape))
      }
    }
    state.error?.let { Text(errorMessage(it), color = MaterialTheme.colorScheme.error) }
    Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val finish = { onAction(Action.Finish) }
      if (state.isLastPage) {
        Box(Modifier)
        Button(onClick = finish, enabled = !state.isSaving) { Text(stringResource(R.string.onboarding_get_started)) }
      } else {
        TextButton(onClick = finish, enabled = !state.isSaving) { Text(stringResource(R.string.onboarding_skip)) }
        Button(onClick = { onAction(Action.Next) }) { Text(stringResource(R.string.onboarding_next)) }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() = ReflexTheme(dynamicColor = false) {
  OnboardingContent(State(page = 1), onAction = {})
}
