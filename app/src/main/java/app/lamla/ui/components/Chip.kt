package app.lamla.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.lamla.ui.theme.lamla

/**
 * Tag/chip — used for course tags, deadline status, filters.
 *
 * Pill-shaped (the one place pills are right). When [color] is provided (a
 * course's accent), the chip becomes a colored dot + label. When not, it's
 * a neutral container chip.
 *
 * Selectable variant flips the bg to the foreground color and inverts text.
 */
@Composable
fun LamlaChip(
    label: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    leadingIcon: ImageVector? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerFull)

    val targetBg = when {
        selected && color != null -> color
        selected -> cs.onSurface
        color != null -> color.copy(alpha = 0.10f)
        else -> cs.surfaceContainer
    }
    val targetFg = when {
        selected && color != null -> Color.White
        selected -> cs.surface
        color != null -> color
        else -> cs.onSurfaceVariant
    }
    val targetBorder = when {
        selected -> Color.Transparent
        color != null -> color.copy(alpha = 0.20f)
        else -> MaterialTheme.lamla.colors.hairline
    }
    val bg by animateColorAsState(targetBg, MaterialTheme.lamla.motion.tweenStandard(150), label = "chip-bg")
    val fg by animateColorAsState(targetFg, MaterialTheme.lamla.motion.tweenStandard(150), label = "chip-fg")
    val br by animateColorAsState(targetBorder, MaterialTheme.lamla.motion.tweenStandard(150), label = "chip-br")

    Row(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, br, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (color != null && leadingIcon == null && !selected) {
            // Color dot for course tags
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(MaterialTheme.lamla.spacing.cornerFull))
                    .background(color)
            )
        }
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
    }
}

/** Thin divider that doesn't get visually heavy in dark mode. */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.lamla.colors.hairline,
    thickness: androidx.compose.ui.unit.Dp = 1.dp
) {
    Box(
        modifier = modifier
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = thickness.toPx()
                )
            }
            .padding(vertical = 0.dp)
    )
}
