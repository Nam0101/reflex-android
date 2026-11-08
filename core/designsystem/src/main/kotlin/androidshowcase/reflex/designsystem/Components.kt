package androidshowcase.reflex.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Outline Poké Ball, used as the brand mark and as a watermark. Drawn opaque in its own layer, then faded by
 * [color]'s alpha, so the strokes do not darken where they cross.
 */
@Composable
fun PokeballMark(modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
  Canvas(modifier.graphicsLayer(alpha = color.alpha)) {
    val solid = color.copy(alpha = 1f)
    val stroke = size.minDimension * 0.075f
    val radius = (size.minDimension - stroke) / 2f
    val inner = radius * 0.3f
    drawCircle(solid, radius, style = Stroke(stroke))
    drawCircle(solid, inner, style = Stroke(stroke))
    drawCircle(solid, inner * 0.42f)
    drawLine(solid, Offset(center.x - radius, center.y), Offset(center.x - inner, center.y), stroke)
    drawLine(solid, Offset(center.x + inner, center.y), Offset(center.x + radius, center.y), stroke)
  }
}

/** Screen title with an optional subtitle and a faint Poké Ball in the corner. */
@Composable
fun LargeHeader(title: String, subtitle: String?, modifier: Modifier = Modifier) {
  Box(modifier.fillMaxWidth()) {
    // matchParentSize keeps the watermark out of the header's height; unbounded lets it overflow the corner.
    PokeballMark(
      Modifier
        .matchParentSize()
        .wrapContentSize(Alignment.TopEnd, unbounded = true)
        .offset(x = 52.dp, y = (-28).dp)
        .size(168.dp)
        .rotate(-18f),
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
    )
    Column(Modifier.padding(top = 24.dp, bottom = 12.dp)) {
      Text(title, style = MaterialTheme.typography.displaySmall)
      if (subtitle != null) {
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun LargeHeaderPreview() = ReflexTheme {
  LargeHeader("Pokédex", "Every Pokémon, page by page", Modifier.padding(horizontal = 16.dp))
}
