package app.lamla.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lamla.ui.theme.lamla

/**
 * The editorial top bar - Lamla's masthead for every secondary screen.
 *
 * Replaces Material's CenterAlignedTopAppBar app-wide for three reasons:
 *   1. The serif title (headlineSmall) sits flush-left beside the back button,
 *      matching the left-aligned editorial headers used everywhere else - the
 *      centered title always fought that grid.
 *   2. The back affordance is a hairline circle, the same language as the
 *      floating hamburger - so the app's chrome reads as one family.
 *   3. One component means the next polish pass is a one-file change.
 *
 * Transparent: the aurora backdrop shows through. Handles the status-bar inset
 * itself, so it drops straight into a Scaffold's topBar slot.
 */
@Composable
fun LamlaTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** Override for modal forms that dismiss rather than navigate back (the ✕). */
    navIcon: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            CircleIconButton(
                icon = { Icon(navIcon, contentDescription = "Back", modifier = Modifier.size(20.dp)) },
                onClick = onBack
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = if (onBack == null) 8.dp else 0.dp)
        )
        actions()
    }
}

/**
 * A hairline-bordered circular icon button - the chrome language shared by the
 * floating hamburger, the top-bar back button, and top-bar actions.
 *
 * Press feedback is the house style: spring scale, no ripple.
 */
@Composable
fun CircleIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = MaterialTheme.lamla.motion.springSnap,
        label = "circle-btn-scale"
    )
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(cs.surfaceContainerLow.copy(alpha = 0.92f), CircleShape)
            .border(1.dp, MaterialTheme.lamla.colors.hairline, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
