package app.lamla.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradient + glow tokens: the "fire" layer.
 *
 * The rest of the design system is intentionally calm (warm neutrals, hairlines).
 * These tokens are the opposite end: a small, disciplined set of vibrant gradients
 * and glow colors used only at hero moments (greeting aura, stress gauge, primary
 * CTA, the live class). Used sparingly they make the app feel alive; used
 * everywhere they'd make it noisy, so callers reach for them deliberately.
 *
 * The signature is the **ember** gradient: a warm sunrise (amber -> coral -> rose)
 * that nods to the "five-to-twelve / last minute" brand warmth. It reads on both
 * light and dark backgrounds, so the ember stops are shared; only the soft auras
 * (which sit *behind* content) differ per mode.
 */
data class LamlaGradients(
    /** Warm signature stops: amber -> amber-orange -> coral -> rose. */
    val ember: List<Color>,
    /** Two-stop warm head for compact arcs/buttons. */
    val emberShort: List<Color>,
    /** Single color for ember glows (colored shadow). */
    val emberGlow: Color,
    /** Cool electric stops for contrast moments. */
    val cool: List<Color>,
    /** Soft hero background aura (top-anchored), low alpha, fades to transparent. */
    val auraWarm: List<Color>,
    /** Soft cool aura variant. */
    val auraCool: List<Color>,
    /** Subtle top sheen for elevated surfaces (highlight -> transparent). */
    val sheen: List<Color>
) {
    /** Diagonal warm gradient sized to the drawing bounds (good for buttons/fills). */
    val emberLinear: Brush get() = Brush.linearGradient(ember)

    /** Horizontal warm gradient (good for thin bars / chips). */
    fun emberHorizontal(): Brush = Brush.horizontalGradient(emberShort)

    /** Top-down warm aura, used as a faint hero background wash. */
    fun warmAura(): Brush = Brush.verticalGradient(auraWarm)

    /** Cool diagonal gradient. */
    val coolLinear: Brush get() = Brush.linearGradient(cool)

    /**
     * A radial aura centered at [center] with [radius] px. Use behind a hero badge
     * or gauge so the glow looks like it emanates from the element.
     */
    fun radialWarm(center: Offset, radius: Float): Brush =
        Brush.radialGradient(colors = auraWarm, center = center, radius = radius)
}

private val LightGradients = LamlaGradients(
    ember = listOf(Palette.EmberGold, Palette.EmberAmber, Palette.EmberCoral, Palette.EmberRose),
    emberShort = listOf(Palette.EmberGold, Palette.EmberCoral),
    emberGlow = Palette.EmberCoral,
    cool = listOf(Palette.Electric, Palette.ElectricSky),
    auraWarm = listOf(
        Palette.EmberGold.copy(alpha = 0.18f),
        Palette.EmberCoral.copy(alpha = 0.07f),
        Color.Transparent
    ),
    auraCool = listOf(
        Palette.Electric.copy(alpha = 0.14f),
        Palette.ElectricSky.copy(alpha = 0.05f),
        Color.Transparent
    ),
    sheen = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)
)

private val DarkGradients = LamlaGradients(
    ember = listOf(Palette.EmberGold, Palette.EmberAmber, Palette.EmberCoral, Palette.EmberRose),
    emberShort = listOf(Palette.EmberGold, Palette.EmberCoral),
    emberGlow = Palette.EmberCoral,
    cool = listOf(Palette.Electric, Palette.ElectricSky),
    // On near-black surfaces the warm aura needs more presence to register.
    auraWarm = listOf(
        Palette.EmberCoral.copy(alpha = 0.24f),
        Palette.EmberGold.copy(alpha = 0.09f),
        Color.Transparent
    ),
    auraCool = listOf(
        Palette.Electric.copy(alpha = 0.22f),
        Palette.ElectricSky.copy(alpha = 0.07f),
        Color.Transparent
    ),
    sheen = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
)

internal fun gradientsFor(dark: Boolean): LamlaGradients = if (dark) DarkGradients else LightGradients

internal val LocalLamlaGradients = staticCompositionLocalOf { LightGradients }
