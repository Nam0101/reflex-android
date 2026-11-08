package androidshowcase.reflex.feature.onboarding.ui

import androidshowcase.reflex.designsystem.PokeballMark
import androidshowcase.reflex.designsystem.ReflexTheme
import androidshowcase.reflex.designsystem.errorMessage
import androidshowcase.reflex.feature.onboarding.OnboardingViewModel
import androidshowcase.reflex.feature.onboarding.OnboardingViewModel.Action
import androidshowcase.reflex.feature.onboarding.OnboardingViewModel.State
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Accents are the Water, Fire and Grass type colors.
private class Page(val icon: ImageVector, val accent: Color, val title: Int, val body: Int)

private val pages = listOf(
  Page(Icons.Filled.Search, Color(0xFF6390F0), R.string.onboarding_page1_title, R.string.onboarding_page1_body),
  Page(Icons.Filled.Favorite, Color(0xFFEE8130), R.string.onboarding_page2_title, R.string.onboarding_page2_body),
  Page(Icons.Filled.Star, Color(0xFF7AC74C), R.string.onboarding_page3_title, R.string.onboarding_page3_body),
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

  val finish = { onAction(Action.Finish) }
  // Surface gives the screen the theme's background and content colors (dark mode included).
  Surface(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
      Box(Modifier.fillMaxWidth().height(48.dp)) {
        if (!state.isLastPage) {
          TextButton(onClick = finish, enabled = !state.isSaving, modifier = Modifier.align(Alignment.CenterEnd)) {
            Text(stringResource(R.string.onboarding_skip))
          }
        }
      }
      HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
        PageContent(pages[index % pages.size])
      }
      PageIndicator(state.pageCount, state.page, Modifier.align(Alignment.CenterHorizontally).padding(vertical = 24.dp))
      state.error?.let {
        Text(errorMessage(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
      }
      Button(
        onClick = { onAction(if (state.isLastPage) Action.Finish else Action.Next) },
        enabled = !state.isSaving,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.large,
      ) {
        val label = if (state.isLastPage) R.string.onboarding_get_started else R.string.onboarding_next
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
      }
    }
  }
}

@Composable
private fun PageContent(page: Page) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    PageArt(page, Modifier.padding(bottom = 16.dp))
    Text(stringResource(page.title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Text(
      stringResource(page.body),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

/** A soft halo around a gradient disc holding the page icon and a Poké Ball watermark. */
@Composable
private fun PageArt(page: Page, modifier: Modifier) {
  val halo = Brush.radialGradient(listOf(page.accent.copy(alpha = 0.3f), Color.Transparent))
  val disc = Brush.linearGradient(listOf(page.accent, lerp(page.accent, Color.Black, 0.25f)))
  Box(modifier.size(260.dp).background(halo), contentAlignment = Alignment.Center) {
    Box(Modifier.size(180.dp).clip(CircleShape).background(disc), contentAlignment = Alignment.Center) {
      PokeballMark(
        Modifier.requiredSize(220.dp).offset(x = 64.dp, y = 56.dp).rotate(-20f),
        color = Color.White.copy(alpha = 0.18f),
      )
      Icon(page.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(84.dp))
    }
  }
}

@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier) {
  val colors = MaterialTheme.colorScheme
  Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    repeat(count) { index ->
      val selected = index == current
      val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "indicatorWidth")
      val color by animateColorAsState(
        if (selected) colors.primary else colors.outlineVariant,
        label = "indicatorColor",
      )
      Box(Modifier.size(width, 8.dp).background(color, CircleShape))
    }
  }
}

@Preview
@Composable
private fun OnboardingPreview() = ReflexTheme {
  OnboardingContent(State(page = 1), onAction = {})
}
