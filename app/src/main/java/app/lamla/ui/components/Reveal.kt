package app.lamla.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Small, reusable motion helpers shared across screens.
 *
 * The app already animates at the container level (nav slide+fade, tab content,
 * press scale). These add the *next* layer: content settling in and data drawing
 * itself, so a freshly-loaded screen feels alive rather than instantly-pasted.
 */

/**
 * Fade + rise-in on first composition. Wrap a list item / card so it eases up into
 * place instead of popping. Pass an increasing [delayMillis] across siblings for a
 * gentle stagger (keep the step small - ~40-60ms - so the screen never feels slow).
 */
@Composable
fun LamlaReveal(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "reveal"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 22.dp.toPx()
        }
    ) {
        content()
    }
}

/**
 * Animate a 0f..1f fraction from 0 up to [target] once the composable appears, so
 * progress bars / charts *draw themselves in*. Re-animates toward [target] if the
 * underlying data changes. [delayMillis] lets sibling bars cascade.
 */
@Composable
fun rememberAppearFraction(
    target: Float,
    durationMs: Int = 650,
    delayMillis: Int = 0
): Float {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        shown = true
    }
    val fraction by animateFloatAsState(
        targetValue = if (shown) target else 0f,
        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        label = "appearFraction"
    )
    return fraction
}
