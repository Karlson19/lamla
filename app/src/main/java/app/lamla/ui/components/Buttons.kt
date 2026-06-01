package app.lamla.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.lamla.ui.theme.glow
import app.lamla.ui.theme.lamla

/**
 * Button variants.
 *
 * Linear-flavoured: small, restrained, no pill shapes. Three intents:
 *   - [LamlaButton]         - primary action (filled, ink/snow background)
 *   - [LamlaSecondaryButton]- secondary action (subtle tonal container)
 *   - [LamlaGhostButton]    - tertiary, walks-away action (text only, no bg)
 *
 * All animate scale 0.97 on press + tonal shift. No ripple - too splashy for
 * this language. Haptic tick lives in callers via [androidx.compose.ui.hapticfeedback].
 */

private enum class ButtonVariant { Primary, Secondary, Ghost, Destructive }

@Composable
fun LamlaButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) = BaseButton(
    label = label,
    onClick = onClick,
    modifier = modifier,
    leadingIcon = leadingIcon,
    enabled = enabled,
    variant = ButtonVariant.Primary
)

@Composable
fun LamlaSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) = BaseButton(
    label = label,
    onClick = onClick,
    modifier = modifier,
    leadingIcon = leadingIcon,
    enabled = enabled,
    variant = ButtonVariant.Secondary
)

@Composable
fun LamlaGhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) = BaseButton(
    label = label,
    onClick = onClick,
    modifier = modifier,
    leadingIcon = leadingIcon,
    enabled = enabled,
    variant = ButtonVariant.Ghost
)

@Composable
fun LamlaDestructiveButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) = BaseButton(
    label = label,
    onClick = onClick,
    modifier = modifier,
    leadingIcon = leadingIcon,
    enabled = enabled,
    variant = ButtonVariant.Destructive
)

@Composable
private fun BaseButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    leadingIcon: ImageVector?,
    enabled: Boolean,
    variant: ButtonVariant
) {
    val cs = MaterialTheme.colorScheme
    // Primary is the ember gradient (white text reads on the warm fill in both
    // light and dark). The rest stay solid/tonal.
    val (bg, fg, border) = when (variant) {
        ButtonVariant.Primary -> Triple(cs.primary, Color.White, Color.Transparent)
        ButtonVariant.Secondary -> Triple(cs.surfaceContainer, cs.onSurface, MaterialTheme.lamla.colors.hairline)
        ButtonVariant.Ghost -> Triple(Color.Transparent, cs.onSurface, Color.Transparent)
        ButtonVariant.Destructive -> Triple(cs.errorContainer, cs.onErrorContainer, Color.Transparent)
    }
    val isPrimary = variant == ButtonVariant.Primary
    val brush = if (isPrimary) MaterialTheme.lamla.gradients.emberLinear else null
    val glowColor = MaterialTheme.lamla.gradients.emberGlow

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = MaterialTheme.lamla.motion.springSnap,
        label = "btn-scale"
    )
    val animatedBg by animateColorAsState(
        targetValue = if (!enabled) bg.copy(alpha = 0.4f)
        else if (pressed) bg.copy(alpha = 0.85f)
        else bg,
        animationSpec = MaterialTheme.lamla.motion.tweenStandard(MaterialTheme.lamla.motion.short3),
        label = "btn-bg"
    )
    val layerAlpha by animateFloatAsState(
        targetValue = if (!enabled) 0.45f else if (pressed) 0.9f else 1f,
        animationSpec = MaterialTheme.lamla.motion.tweenStandard(MaterialTheme.lamla.motion.short3),
        label = "btn-alpha"
    )

    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerSm)

    Box(
        modifier = modifier
            .scale(scale)
            .then(if (isPrimary && enabled) Modifier.glow(glowColor, shape, radius = 13.dp, alpha = 0.42f) else Modifier)
            .alpha(layerAlpha)
            .clip(shape)
            .then(
                if (brush != null) Modifier.background(brush, shape)
                else Modifier.background(animatedBg, shape)
            )
            .then(
                if (border != Color.Transparent)
                    Modifier.border(BorderStroke(1.dp, border), shape)
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (enabled) fg else fg.copy(alpha = 0.4f),
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
            CompositionLocalProvider(LocalContentColor provides if (enabled) fg else fg.copy(alpha = 0.4f)) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    Text(label)
                }
            }
        }
    }
}
