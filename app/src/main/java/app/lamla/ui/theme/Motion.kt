package app.lamla.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Motion tokens.
 *
 * Three principles:
 *   1. Spring physics for state changes (a card snapping into "done" state should
 *      feel physical, not timed).
 *   2. Tweened durations for *transitions* (screen ↔ screen, list reorder) - predictable,
 *      cache-friendly.
 *   3. Material 3 standard easing curves, named so callers don't import private APIs.
 *
 * Durations match Material 3 motion duration tokens but are exposed by intent.
 */
data class LamlaMotion(
    // Spring presets ---------------------------------------------------------
    /** Snappy: press states, toggles. */
    val springSnap: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    ),
    /** Bouncy: success confirmations, sheet entrances. */
    val springBouncy: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
    /** Gentle: list reorders, surface elevation. */
    val springGentle: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessVeryLow
    ),
    /** Stiff but non-bouncy - generic value animations. */
    val springStandard: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    ),

    // Tween durations (ms) ---------------------------------------------------
    val short1: Int = 50,
    val short2: Int = 100,
    val short3: Int = 150,
    val short4: Int = 200,
    val medium1: Int = 250,
    val medium2: Int = 300,
    val medium3: Int = 350,
    val medium4: Int = 400,
    val long1: Int = 450,
    val long2: Int = 500,
    val long3: Int = 550,
    val long4: Int = 600,
    val extraLong1: Int = 700,
    val extraLong2: Int = 800,

    // Easings ----------------------------------------------------------------
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
    val accelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
) {
    fun <T> tweenStandard(durationMs: Int = medium2): AnimationSpec<T> =
        tween(durationMillis = durationMs, easing = standard)

    fun <T> tweenEmphasized(durationMs: Int = long2): AnimationSpec<T> =
        tween(durationMillis = durationMs, easing = emphasized)
}

internal val Motion = LamlaMotion()
internal val LocalLamlaMotion = staticCompositionLocalOf { Motion }

/**
 * Elevation tokens - used sparingly. Material 3 maps elevation to tonal tint;
 * we add subtle 1dp borders instead of heavy shadows.
 */
data class LamlaElevation(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp
)

internal val Elevation = LamlaElevation()
internal val LocalLamlaElevation = staticCompositionLocalOf { Elevation }
