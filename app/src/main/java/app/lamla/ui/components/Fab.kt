package app.lamla.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lamla.ui.theme.glow
import app.lamla.ui.theme.lamla

/**
 * THE floating action button - an ember-lit circle with a warm halo.
 *
 * One creation gesture, one look: Home's quick capture, Timetable's add class,
 * Courses/Deadlines/Lecturers/Exams' add buttons all use this. Before this
 * existed half the app used ink-filled Material FABs and the other half rolled
 * the ember circle by hand - same action, two languages.
 *
 * Press feedback is the house style: spring scale, no ripple.
 */
@Composable
fun LamlaFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Add,
    contentDescription: String? = null,
    size: Dp = 52.dp
) {
    val gradients = MaterialTheme.lamla.gradients
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = MaterialTheme.lamla.motion.springSnap,
        label = "fab-scale"
    )
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .glow(gradients.emberGlow, CircleShape, radius = 16.dp, alpha = 0.5f)
            .clip(CircleShape)
            .background(gradients.emberLinear, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size * 0.4f)
        )
    }
}
