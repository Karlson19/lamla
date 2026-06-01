package app.lamla.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Depth + glow modifiers.
 *
 * The base design language avoids shadows (it differentiates surfaces with tint +
 * hairline). These modifiers are the opt-in exception for hero elements that should
 * feel lifted or lit:
 *
 *   - [softElevation]: a soft, neutral ambient shadow for floating cards.
 *   - [glow]: a colored halo (the ember/accent "fire") around a key element.
 *
 * Both use Compose's [shadow], whose [androidx.compose.ui.graphics.Shadow] color is
 * honored on API 28+. On 26-27 it falls back to a neutral elevation shadow, which
 * still reads as depth, so the floor stays graceful.
 */

/** Soft neutral elevation for floating cards / sheets. Cheap and tasteful. */
fun Modifier.softElevation(
    shape: Shape = RoundedCornerShape(20.dp),
    radius: Dp = 16.dp,
    color: Color = Color.Black
): Modifier = this.shadow(
    elevation = radius,
    shape = shape,
    clip = false,
    ambientColor = color.copy(alpha = 0.10f),
    spotColor = color.copy(alpha = 0.16f)
)

/**
 * A colored glow halo around [shape]. Renders the shadow *outside* the element
 * (clip = false) so it reads as light spilling out. Tune [radius] for spread and
 * [alpha] for intensity.
 */
fun Modifier.glow(
    color: Color,
    shape: Shape = RoundedCornerShape(20.dp),
    radius: Dp = 22.dp,
    alpha: Float = 0.5f
): Modifier = this.shadow(
    elevation = radius,
    shape = shape,
    clip = false,
    ambientColor = color.copy(alpha = alpha * 0.7f),
    spotColor = color.copy(alpha = alpha)
)

/**
 * The standard screen backdrop: the solid app background with a soft aurora bloom
 * radiating from the top-center, fixed to the viewport so it stays put as content
 * scrolls beneath it.
 *
 * This is the single move that ties every screen into the same "lit from above"
 * world. Apply it to a screen's outermost fillMaxSize container (or directly to a
 * full-bleed LazyColumn) in place of a plain `.background(colorScheme.background)`.
 * Pass [cool] for the electric-blue variant on screens that want a cooler mood.
 */
@Composable
fun Modifier.auroraBackdrop(cool: Boolean = false): Modifier {
    val aura = if (cool) MaterialTheme.lamla.gradients.auraCool
    else MaterialTheme.lamla.gradients.auraWarm
    val background = MaterialTheme.colorScheme.background
    return this
        .background(background)
        .drawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = aura,
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.height * 0.5f
                )
            )
        }
}
