package androidshowcase.reflex.designsystem

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * What a shared element needs: the layout the transition plays in and the visibility of the screen holding the
 * element. The navigation host provides one per screen; previews and tests have none.
 */
class SharedTransition(val layout: SharedTransitionScope, val screen: AnimatedVisibilityScope)

val LocalSharedTransition = staticCompositionLocalOf<SharedTransition?> { null }

/**
 * Animates this element to the one with the same [key] on the next screen (and back); a no-op without a host.
 * Built on `sharedBounds`, so the two cross-fade: the target still shows the outgoing image while its own decodes.
 */
@Composable
fun Modifier.sharedElement(key: Any): Modifier = shared(key, ResizeMode.scaleToBounds(), zIndexInOverlay = 1f)

/**
 * Morphs this background into the one with the same [key] on the next screen. It is re-measured while it moves,
 * so corner radii and gradients stay crisp, and it is drawn below the shared elements it carries.
 */
@Composable
fun Modifier.sharedBackground(key: Any): Modifier = shared(key, ResizeMode.RemeasureToBounds, zIndexInOverlay = 0f)

@Composable
private fun Modifier.shared(key: Any, resizeMode: ResizeMode, zIndexInOverlay: Float): Modifier {
  val shared = LocalSharedTransition.current ?: return this
  return with(shared.layout) {
    this@shared.sharedBounds(
      rememberSharedContentState(key),
      shared.screen,
      resizeMode = resizeMode,
      zIndexInOverlay = zIndexInOverlay,
    )
  }
}
