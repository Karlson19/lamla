package app.lamla.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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

        // A little face that lives the score with you: calm and smiling on a Chill
        // day, sweating and jittering when it's Crunch. Pure motion, no extra data.
        StressMascot(band = band, accent = animatedAccent)
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

/**
 * A tiny round mascot that emotes the current [StressBand]. It is pure motion — it
 * carries no number, it just *feels* the day: smiling and gently bobbing on a Chill
 * day, brows knit and jittering with a falling sweat-drop when it's Crunch.
 *
 * Everything is drawn on one Canvas so it stays cheap. Four infinite transitions drive
 * it: a vertical bob (slow when calm, fast when frantic), a horizontal nervous jitter
 * (Heavy/Crunch only), an occasional blink, and a sweat-drop drip (Heavy/Crunch only).
 */
@Composable
private fun StressMascot(band: StressBand, accent: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mascot")

    // Idle bob: a calm float when Chill, an anxious twitch when Crunch.
    val bobMs = when (band) {
        StressBand.Chill -> 2200
        StressBand.Steady -> 1700
        StressBand.Heavy -> 1100
        StressBand.Crunch -> 600
    }
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(bobMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot-bob"
    )
    // Nervous side-to-side jitter, only when the load is real.
    val jitterAmp = when (band) {
        StressBand.Heavy -> 0.5f
        StressBand.Crunch -> 1.2f
        else -> 0f
    }
    val jitter by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (band == StressBand.Crunch) 90 else 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot-jitter"
    )
    // Blink: eyes shut for a blink near the end of each cycle.
    val blinkPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (band == StressBand.Crunch) 1800 else 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mascot-blink"
    )
    // A bead of sweat that drips and resets — only when stressed.
    val sweatPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mascot-sweat"
    )

    // Mouth curvature: +1 = big smile, -1 = full frown.
    val smile = when (band) {
        StressBand.Chill -> 1f
        StressBand.Steady -> 0.35f
        StressBand.Heavy -> -0.4f
        StressBand.Crunch -> -1f
    }
    val showBrows = band == StressBand.Heavy || band == StressBand.Crunch
    val showSweat = band == StressBand.Heavy || band == StressBand.Crunch

    // Resolve colors outside the draw lambda (DrawScope can't read the theme).
    val bodyFill = accent.copy(alpha = 0.20f)
    val bodyStroke = accent
    val ink = MaterialTheme.colorScheme.onSurface
    val sweatColor = lerp(accent, Color(0xFF7EC8E3), 0.7f)

    Canvas(modifier = modifier.size(54.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f + jitter * jitterAmp * (w * 0.03f)
        val cy = h / 2f + bob * (h * 0.05f)
        val headR = w * 0.40f

        // Body.
        drawCircle(color = bodyFill, radius = headR, center = Offset(cx, cy))
        drawCircle(
            color = bodyStroke,
            radius = headR,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.05f)
        )

        // Eyes (rounded bars that squash shut on a blink).
        val eyeDx = headR * 0.42f
        val eyeY = cy - headR * 0.10f
        val eyeR = headR * 0.13f
        val open = blinkOpenness(blinkPhase).coerceAtLeast(0.08f)
        for (sx in listOf(-1f, 1f)) {
            val ex = cx + sx * eyeDx
            val eh = (eyeR * 2f) * open
            drawRoundRect(
                color = ink,
                topLeft = Offset(ex - eyeR, eyeY - eh / 2f),
                size = Size(eyeR * 2f, eh),
                cornerRadius = CornerRadius(eyeR, eyeR)
            )
        }

        // Worried/angry brows on the harder bands (\  / shape).
        if (showBrows) {
            val browY = eyeY - headR * 0.34f
            val browHalf = headR * 0.20f
            val slant = if (band == StressBand.Crunch) headR * 0.18f else headR * 0.11f
            val sw = w * 0.045f
            // Left brow: outer high, inner low.
            drawLine(
                color = ink,
                start = Offset(cx - eyeDx - browHalf, browY - slant),
                end = Offset(cx - eyeDx + browHalf, browY + slant),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
            // Right brow mirrors it.
            drawLine(
                color = ink,
                start = Offset(cx + eyeDx - browHalf, browY + slant),
                end = Offset(cx + eyeDx + browHalf, browY - slant),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
        }

        // Mouth: a quadratic curve whose bow is driven by [smile].
        val mouthY = cy + headR * 0.36f
        val mouthHalf = headR * 0.40f
        val bow = smile * headR * 0.36f
        val mouth = Path().apply {
            moveTo(cx - mouthHalf, mouthY)
            quadraticBezierTo(cx, mouthY + bow, cx + mouthHalf, mouthY)
        }
        drawPath(mouth, color = ink, style = Stroke(width = w * 0.05f, cap = StrokeCap.Round))

        // Falling sweat bead.
        if (showSweat) {
            val dropX = cx + headR * 0.78f
            val startY = cy - headR * 0.45f
            val dy = sweatPhase * headR * 1.2f
            val alpha = (1f - sweatPhase).coerceIn(0f, 1f)
            drawCircle(
                color = sweatColor.copy(alpha = alpha),
                radius = headR * 0.13f,
                center = Offset(dropX, startY + dy)
            )
        }
    }
}

/** Eyes open ~90% of the cycle, then a quick V-shaped blink (1 → 0 → 1). */
private fun blinkOpenness(phase: Float): Float =
    if (phase > 0.90f) {
        val t = (phase - 0.90f) / 0.10f       // 0..1 across the blink window
        kotlin.math.abs(t - 0.5f) * 2f         // 1 -> 0 -> 1
    } else 1f

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
