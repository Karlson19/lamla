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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.glow
import app.lamla.ui.theme.lamla
import app.lamla.ui.theme.softElevation
import kotlin.math.roundToInt

/**
 * Stress score, redesigned as a hero gauge.
 *
 * A bold circular gauge (gradient arc + colored glow) carries the number, with the
 * band label and a one-line read on the right. The arc and glow are tinted by the
 * band, so a Crunch day literally glows hot while a Chill day glows soft green; the
 * number counts up as the score animates in. The whole card floats on a soft warm
 * aura. This is the screen's focal point, so it earns the visual weight.
 *
 * @param score      0-100 normalized stress score
 * @param band       Stress band, drives label + accent color
 * @param onClick    Tap to open the breakdown
 */
@Composable
fun StressIndicator(
    score: Int,
    band: StressBand,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val target = score.coerceIn(0, 100).toFloat() / 100f
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = MaterialTheme.lamla.motion.springGentle,
        label = "stress-progress"
    )
    // Count the number up alongside the arc fill.
    val displayed = (progress * 100f).roundToInt()

    val accent = bandColor(band)
    val animatedAccent by animateColorAsState(
        targetValue = accent,
        animationSpec = MaterialTheme.lamla.motion.tweenStandard(MaterialTheme.lamla.motion.medium2),
        label = "stress-accent"
    )
    val shape = RoundedCornerShape(MaterialTheme.lamla.spacing.cornerLg)
    val aura = MaterialTheme.lamla.gradients.auraWarm

    Row(
        modifier = modifier
            .fillMaxWidth()
            .softElevation(shape, radius = 18.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            // Faint warm aura blooming from the gauge side.
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = aura,
                        center = Offset(size.width * 0.20f, size.height * 0.5f),
                        radius = size.height * 1.1f
                    ),
                    center = Offset(size.width * 0.20f, size.height * 0.5f),
                    radius = size.height * 1.1f
                )
            }
            .border(1.dp, MaterialTheme.lamla.colors.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        StressGauge(progress = progress, displayed = displayed, accent = animatedAccent)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Today's load".uppercase(),
                style = LamlaTextStyles.SectionLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = band.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = band.read,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StressGauge(progress: Float, displayed: Int, accent: Color) {
    val track = MaterialTheme.lamla.colors.timelineRail
    val accentLight = lerp(accent, Color.White, 0.42f)
    val gaugeSize = 104.dp

    Box(
        modifier = Modifier
            .size(gaugeSize)
            .glow(accent, androidx.compose.foundation.shape.CircleShape, radius = 18.dp, alpha = 0.45f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 11.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            // Track (full ring, faint).
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Progress arc with a gradient sweep in the band color.
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(accentLight, accent, accentLight),
                        center = Offset(size.width / 2f, size.height / 2f)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = displayed.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "/100",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 6.dp, start = 1.dp)
            )
        }
    }
}

enum class StressBand(val label: String, val read: String, val min: Int, val max: Int) {
    Chill("Chill", "Breathing room. Enjoy it.", 0, 24),
    Steady("Steady", "A manageable rhythm.", 25, 49),
    Heavy("Heavy", "Plenty on. Pace yourself.", 50, 74),
    Crunch("Crunch", "Crunch time. Triage hard.", 75, 100);

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
