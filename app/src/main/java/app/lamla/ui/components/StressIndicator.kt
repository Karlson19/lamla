package app.lamla.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.lamla
import kotlin.math.roundToInt

/**
 * Stress score display.
 *
 * Top of the home screen. Renders:
 *   - Big tabular number (the score)
 *   - Band label ("Steady", "Crunch", etc.)
 *   - Subtle progress bar showing where today sits in the band
 *   - On tap → opens the breakdown sheet (the parent handles that)
 *
 * Visual treatment: the *bar* is the color, not the number. The number stays
 * neutral so it can read at any band. This is restrained — versus the typical
 * traffic-light score that screams red at you.
 *
 * @param score      0–100 normalized stress score
 * @param band       Stress band, drives label + accent color
 * @param onClick    Tap → breakdown
 */
@Composable
fun StressIndicator(
    score: Int,
    band: StressBand,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val target = score.coerceIn(0, 100).toFloat() / 100f
    val animatedScore by animateFloatAsState(
        targetValue = target,
        animationSpec = MaterialTheme.lamla.motion.springGentle,
        label = "stress-progress"
    )

    val accent = bandColor(band)
    val borderColor = MaterialTheme.lamla.colors.hairline
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerLg)

    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Today's load".uppercase(),
                    style = LamlaTextStyles.SectionLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }
            }
            // Band label, right-aligned
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(MaterialTheme.lamla.spacing.cornerFull))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = band.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
        }
        StressBar(progress = animatedScore, color = accent)
    }
}

@Composable
private fun StressBar(progress: Float, color: Color) {
    val railColor = MaterialTheme.lamla.colors.timelineRail
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = MaterialTheme.lamla.motion.tweenStandard(300),
        label = "stress-bar-color"
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
    ) {
        val h = size.height
        val w = size.width
        // Rail
        drawRoundRect(
            color = railColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2)
        )
        // Filled progress
        drawRoundRect(
            color = animatedColor,
            size = Size(width = w * progress, height = h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2)
        )
        // Tick marks at band thresholds (25 / 50 / 75) — subtle, only on rail
        listOf(0.25f, 0.5f, 0.75f).forEach { t ->
            if (t > progress) {
                drawLine(
                    color = railColor.copy(alpha = 0.5f),
                    start = Offset(w * t, 0f),
                    end = Offset(w * t, h),
                    strokeWidth = 1f
                )
            }
        }
    }
}

enum class StressBand(val label: String, val min: Int, val max: Int) {
    Chill("Chill", 0, 24),
    Steady("Steady", 25, 49),
    Heavy("Heavy", 50, 74),
    Crunch("Crunch", 75, 100);

    companion object {
        fun fromScore(score: Int): StressBand = when (score.coerceIn(0, 100)) {
            in 0..24 -> Chill
            in 25..49 -> Steady
            in 50..74 -> Heavy
            else -> Crunch
        }
    }
}

@Composable
private fun bandColor(band: StressBand): Color = when (band) {
    StressBand.Chill -> MaterialTheme.lamla.colors.stressChill
    StressBand.Steady -> MaterialTheme.lamla.colors.stressSteady
    StressBand.Heavy -> MaterialTheme.lamla.colors.stressHeavy
    StressBand.Crunch -> MaterialTheme.lamla.colors.stressCrunch
}
