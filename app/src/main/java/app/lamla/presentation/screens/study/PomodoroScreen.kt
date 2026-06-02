package app.lamla.presentation.screens.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lamla.ui.components.*
import app.lamla.ui.theme.LamlaTextStyles
import app.lamla.ui.theme.auroraBackdrop
import app.lamla.ui.theme.glow
import app.lamla.ui.theme.lamla
import kotlin.math.min

/**
 * Pomodoro screen.
 *
 * Visual centerpiece: a giant ring + mono-figure timer. The ring tracks remaining
 * time within the current phase; below it, three pip markers show position in the
 * 4-cycle long-break cadence (one pip lights for each completed focus block).
 *
 * Phases: Focus → Short break → Focus → Short → Focus → Short → Focus → LONG break → repeat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    onBack: () -> Unit,
    viewModel: PomodoroViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize().auroraBackdrop(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(state.phaseLabel, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.lamla.spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.3f))

            // Ring progress as an Animatable so we control the transition: it glides
            // forward second-to-second, but *snaps* to 0 at a phase boundary instead
            // of sweeping backward from full (the old rewind that read as a glitch).
            val ringProgress = remember { Animatable(0f) }
            LaunchedEffect(state.phase, state.totalSeconds) {
                ringProgress.snapTo(state.progress)
            }
            LaunchedEffect(state.progress) {
                ringProgress.animateTo(state.progress, animationSpec = tween(950, easing = LinearEasing))
            }

            // A gentle "breathing" pulse while a focus block runs, so the screen feels
            // alive rather than frozen. Stops during breaks and when paused.
            val breathe = rememberInfiniteTransition(label = "pomo-breathe")
            val breath by breathe.animateFloat(
                initialValue = 1f,
                targetValue = 1.035f,
                animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
                label = "breath"
            )
            val running = state.isRunning && !state.isBreak

            // Ring + timer. A colored halo spills from the ring; it burns brightest
            // during a running focus block and dims to an ember during breaks/pauses.
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .scale(if (running) breath else 1f)
                    .glow(
                        color = MaterialTheme.lamla.gradients.emberGlow,
                        shape = CircleShape,
                        radius = 56.dp,
                        alpha = if (state.isRunning) (if (state.isBreak) 0.28f else 0.55f) else 0.12f
                    ),
                contentAlignment = Alignment.Center
            ) {
                ProgressRing(
                    progress = ringProgress.value,
                    isBreak = state.isBreak,
                    accent = MaterialTheme.colorScheme.onSurface
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.timerLabel,
                        style = LamlaTextStyles.Timer,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.phaseLabel.uppercase(),
                        style = LamlaTextStyles.SectionLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.size(24.dp))

            // Cycle pips - position within the current long-break cadence. Resets each
            // cadence (the old `i < completedFocusCycles` filled forever and never reset
            // after a long break). A pip pops as it lights.
            val cadencePos = state.completedFocusCycles % state.cyclesUntilLong
            val pipsFilled = if (state.completedFocusCycles > 0 && cadencePos == 0) state.cyclesUntilLong else cadencePos
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(state.cyclesUntilLong) { i ->
                    val filled = i < pipsFilled
                    val pipColor by animateColorAsState(
                        if (filled) MaterialTheme.colorScheme.onSurface else MaterialTheme.lamla.colors.hairlineStrong,
                        MaterialTheme.lamla.motion.tweenStandard(MaterialTheme.lamla.motion.medium2),
                        label = "pipColor"
                    )
                    val pipScale by animateFloatAsState(if (filled) 1.25f else 1f, MaterialTheme.lamla.motion.springBouncy, label = "pipScale")
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(pipScale)
                            .clip(CircleShape)
                            .background(pipColor)
                    )
                }
            }
            Spacer(Modifier.weight(0.4f))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LamlaSecondaryButton(
                    label = "Reset",
                    leadingIcon = Icons.Outlined.Refresh,
                    onClick = { viewModel.reset() },
                    modifier = Modifier.weight(1f)
                )
                LamlaButton(
                    label = if (state.isRunning) "Pause" else "Start",
                    leadingIcon = if (state.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    onClick = { viewModel.toggle() },
                    modifier = Modifier.weight(2f)
                )
                LamlaSecondaryButton(
                    label = "Skip",
                    leadingIcon = Icons.Outlined.SkipNext,
                    onClick = { viewModel.skip() },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, isBreak: Boolean, accent: Color) {
    val railColor = MaterialTheme.lamla.colors.timelineRail
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 14.dp.toPx()
        val diameter = min(size.width, size.height) - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val ringSize = Size(diameter, diameter)

        // Rail
        drawArc(
            color = railColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = ringSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // Progress
        drawArc(
            color = if (isBreak) accent.copy(alpha = 0.5f) else accent,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = ringSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
