package app.lamla.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.scale
import app.lamla.ui.theme.lamla

/**
 * Foundation surface for cards, sheets, list rows.
 *
 * Two principles:
 *   1. Differentiate surfaces by **bg tint + 1dp border**, not shadow. The border
 *      is what gives a card its edge on the off-white/ink background — shadows
 *      look wrong on warm-neutral palettes.
 *   2. If clickable, animate a subtle press scale (0.98) + tint shift. No ripple
 *      flash — too loud for this design language.
 *
 * Use over [androidx.compose.material3.Card] when you want a non-clickable surface
 * or want to opt out of Material's elevation tonal-tint behavior.
 */
@Composable
fun LamlaSurface(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.lamla.colors.hairline,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = MaterialTheme.lamla.spacing.cornerMd,
    contentPadding: Dp = MaterialTheme.lamla.spacing.lg,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = MaterialTheme.lamla.motion.springSnap,
        label = "surface-scale"
    )
    val bg by animateColorAsState(
        targetValue = if (pressed) MaterialTheme.colorScheme.surfaceContainerHigh else color,
        animationSpec = MaterialTheme.lamla.motion.tweenStandard(MaterialTheme.lamla.motion.short3),
        label = "surface-bg"
    )

    val base = modifier
        .scale(scale)
        .clip(shape)
        .background(bg, shape)
        .border(BorderStroke(borderWidth, borderColor), shape)

    val withClick = if (onClick != null) {
        base.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else base

    androidx.compose.foundation.layout.Box(
        modifier = withClick.padding(contentPadding)
    ) {
        content()
    }
}
