package app.lamla.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Themes. Picked by the user, never auto-imposed.
 *
 * Neutral set:
 * - [System]      — follows the device dark-mode setting (default).
 * - [Light]       — warm ivory + ink, gold accent.
 * - [Dark]        — cool obsidian + snow, gold accent.
 * - [Gold]        — light variant where the accent dominates more strongly (KNUST campus feel).
 * - [Monochrome]  — accent collapses to ink/snow, for users who want zero color noise.
 *
 * Vibrant set — each adapts to light & dark automatically via the system setting:
 * - [Indigo] [Emerald] [Teal] [Ocean] [Sunset] [Crimson] [Rose] [Lavender] [Plum]
 *
 * Every vibrant accent uses a 600-700-level primary in light mode (white text) and a
 * 300-level primary in dark mode (ink text), so label contrast stays >= 4.5:1.
 */
enum class AppTheme {
    System, Light, Dark, Gold, Monochrome,
    Indigo, Emerald, Teal, Ocean, Sunset, Crimson, Rose, Lavender, Plum
}

/**
 * A representative accent color for theme-picker swatches. `null` for the neutral
 * themes (System/Light/Dark/Monochrome) that don't have a single accent.
 */
fun AppTheme.swatch(): Color? = when (this) {
    AppTheme.System, AppTheme.Light, AppTheme.Dark, AppTheme.Monochrome -> null
    AppTheme.Gold -> Palette.Gold
    AppTheme.Indigo -> Color(0xFF4F46E5)
    AppTheme.Emerald -> Color(0xFF059669)
    AppTheme.Teal -> Color(0xFF0D9488)
    AppTheme.Ocean -> Color(0xFF0284C7)
    AppTheme.Sunset -> Color(0xFFEA580C)
    AppTheme.Crimson -> Color(0xFFDC2626)
    AppTheme.Rose -> Color(0xFFDB2777)
    AppTheme.Lavender -> Color(0xFF8B5CF6)
    AppTheme.Plum -> Color(0xFFA21CAF)
}

@Composable
fun LamlaTheme(
    theme: AppTheme = AppTheme.System,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) {
        AppTheme.Light, AppTheme.Gold -> false
        AppTheme.Dark -> true
        else -> systemDark        // System, Monochrome, and every vibrant theme follow the system
    }

    val targetScheme = when (theme) {
        AppTheme.System, AppTheme.Light, AppTheme.Dark -> if (dark) DarkScheme else LightScheme
        AppTheme.Gold -> GoldScheme
        AppTheme.Monochrome -> if (dark) MonoDarkScheme else MonoLightScheme
        AppTheme.Indigo -> if (dark) IndigoDark else IndigoLight
        AppTheme.Emerald -> if (dark) EmeraldDark else EmeraldLight
        AppTheme.Teal -> if (dark) TealDark else TealLight
        AppTheme.Ocean -> if (dark) OceanDark else OceanLight
        AppTheme.Sunset -> if (dark) SunsetDark else SunsetLight
        AppTheme.Crimson -> if (dark) CrimsonDark else CrimsonLight
        AppTheme.Rose -> if (dark) RoseDark else RoseLight
        AppTheme.Lavender -> if (dark) LavenderDark else LavenderLight
        AppTheme.Plum -> if (dark) PlumDark else PlumLight
    }

    // Cross-fade the whole scheme when the user switches theme (or the system flips
    // light/dark) so the change glides instead of hard-cutting.
    val colorScheme = animateColorScheme(targetScheme, Motion.tweenStandard(Motion.medium3))

    val extended = when (theme) {
        AppTheme.Gold -> GoldExtendedColors
        AppTheme.Monochrome -> if (dark) DarkExtendedColors.asMonochrome() else LightExtendedColors.asMonochrome()
        else -> (if (dark) DarkExtendedColors else LightExtendedColors).copy(timelineNow = colorScheme.primary)
    }

    // System bars: transparent, with correct icon contrast.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val isLightSurface = colorScheme.background.luminance() > 0.5f
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = isLightSurface
                isAppearanceLightNavigationBars = isLightSurface
            }
        }
    }

    CompositionLocalProvider(
        LocalLamlaColors provides extended,
        LocalLamlaSpacing provides Spacing,
        LocalLamlaMotion provides Motion,
        LocalLamlaElevation provides Elevation
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LamlaTypography,
            shapes = LamlaShapes,
            content = content
        )
    }
}

// region — Material 3 ColorScheme definitions ----------------------------------

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Palette.Ink,
    onPrimary = Palette.Ivory,
    primaryContainer = Palette.GoldSoft,
    onPrimaryContainer = Palette.Ink,

    secondary = Palette.Gold,
    onSecondary = Palette.Ink,
    secondaryContainer = Palette.Bone,
    onSecondaryContainer = Palette.Graphite,

    tertiary = Palette.GoldDeep,
    onTertiary = Palette.Ivory,
    tertiaryContainer = Palette.GoldSoft,
    onTertiaryContainer = Palette.Ink,

    background = Palette.Ivory,
    onBackground = Palette.Ink,
    surface = Palette.Ivory,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.Bone,
    onSurfaceVariant = Palette.Graphite,
    surfaceTint = Palette.Gold,

    surfaceContainerLowest = Palette.Ivory,
    surfaceContainerLow = Color(0xFFF7F6F1),
    surfaceContainer = Palette.Bone,
    surfaceContainerHigh = Color(0xFFEEEDE7),
    surfaceContainerHighest = Palette.Linen,

    outline = Palette.Stone,
    outlineVariant = Color(0xFFE5E2DA),

    error = Palette.Rust,
    onError = Palette.Ivory,
    errorContainer = Color(0xFFF4D7CF),
    onErrorContainer = Color(0xFF5C2418),

    scrim = Color(0x66000000)
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Palette.Snow,
    onPrimary = Palette.Obsidian,
    primaryContainer = Palette.GoldGhost,
    onPrimaryContainer = Palette.GoldSoft,

    secondary = Palette.Gold,
    onSecondary = Palette.Obsidian,
    secondaryContainer = Palette.Charcoal,
    onSecondaryContainer = Palette.Mist,

    tertiary = Palette.GoldSoft,
    onTertiary = Palette.Obsidian,
    tertiaryContainer = Palette.GoldGhost,
    onTertiaryContainer = Palette.GoldSoft,

    background = Palette.Obsidian,
    onBackground = Palette.Snow,
    surface = Palette.Obsidian,
    onSurface = Palette.Snow,
    surfaceVariant = Palette.Charcoal,
    onSurfaceVariant = Palette.Mist,
    surfaceTint = Palette.Gold,

    surfaceContainerLowest = Color(0xFF06060A),
    surfaceContainerLow = Color(0xFF0F0F14),
    surfaceContainer = Palette.Charcoal,
    surfaceContainerHigh = Color(0xFF17171D),
    surfaceContainerHighest = Palette.Onyx,

    outline = Palette.Iron,
    outlineVariant = Color(0xFF22222B),

    error = Palette.RustDark,
    onError = Palette.Obsidian,
    errorContainer = Color(0xFF3A1B14),
    onErrorContainer = Color(0xFFF4D7CF),

    scrim = Color(0xCC000000)
)

/** Gold theme — same warm light base but the accent is foregrounded more (chips, headers). */
private val GoldScheme: ColorScheme = LightScheme.copy(
    primary = Palette.GoldDeep,
    onPrimary = Palette.Ivory,
    secondary = Palette.GoldDeep,
    surfaceTint = Palette.GoldDeep
)

private val MonoLightScheme: ColorScheme = LightScheme.copy(
    primary = Palette.Ink,
    secondary = Palette.Graphite,
    tertiary = Palette.Slate,
    primaryContainer = Palette.Linen,
    secondaryContainer = Palette.Linen,
    tertiaryContainer = Palette.Linen,
    surfaceTint = Palette.Ink
)

private val MonoDarkScheme: ColorScheme = DarkScheme.copy(
    primary = Palette.Snow,
    secondary = Palette.Mist,
    tertiary = Palette.Ash,
    primaryContainer = Palette.Iron,
    secondaryContainer = Palette.Iron,
    tertiaryContainer = Palette.Iron,
    surfaceTint = Palette.Snow
)

// -- Vibrant themes ------------------------------------------------------------
// Each is the neutral Light/Dark scheme with the accent slots swapped. Surfaces
// stay near-neutral so content reads cleanly; the accent carries the personality.

private fun accentLight(primary: Color, onPrimary: Color, container: Color, onContainer: Color): ColorScheme =
    LightScheme.copy(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = container, onPrimaryContainer = onContainer,
        secondary = primary, onSecondary = onPrimary,
        secondaryContainer = container, onSecondaryContainer = onContainer,
        tertiary = primary, onTertiary = onPrimary,
        tertiaryContainer = container, onTertiaryContainer = onContainer,
        surfaceTint = primary
    )

private fun accentDark(primary: Color, onPrimary: Color, container: Color, onContainer: Color): ColorScheme =
    DarkScheme.copy(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = container, onPrimaryContainer = onContainer,
        secondary = primary, onSecondary = onPrimary,
        secondaryContainer = container, onSecondaryContainer = onContainer,
        tertiary = primary, onTertiary = onPrimary,
        tertiaryContainer = container, onTertiaryContainer = onContainer,
        surfaceTint = primary
    )

private val IndigoLight   = accentLight(Color(0xFF4F46E5), Color.White, Color(0xFFE0E0FF), Color(0xFF1E1B4B))
private val IndigoDark    = accentDark(Color(0xFFA5B4FC), Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFFE0E7FF))

private val EmeraldLight  = accentLight(Color(0xFF059669), Color.White, Color(0xFFCDEFE0), Color(0xFF064E3B))
private val EmeraldDark   = accentDark(Color(0xFF6EE7B7), Color(0xFF06281E), Color(0xFF065F46), Color(0xFFD1FAE5))

private val TealLight     = accentLight(Color(0xFF0D9488), Color.White, Color(0xFFCCEFEA), Color(0xFF134E4A))
private val TealDark      = accentDark(Color(0xFF5EEAD4), Color(0xFF042F2A), Color(0xFF115E59), Color(0xFFCCFBF1))

private val OceanLight    = accentLight(Color(0xFF0284C7), Color.White, Color(0xFFD0EBFB), Color(0xFF0C4A6E))
private val OceanDark     = accentDark(Color(0xFF7DD3FC), Color(0xFF082F49), Color(0xFF075985), Color(0xFFE0F2FE))

private val SunsetLight   = accentLight(Color(0xFFEA580C), Color.White, Color(0xFFFFE0D0), Color(0xFF7C2D12))
private val SunsetDark    = accentDark(Color(0xFFFDBA74), Color(0xFF431407), Color(0xFF9A3412), Color(0xFFFFEDD5))

private val CrimsonLight  = accentLight(Color(0xFFDC2626), Color.White, Color(0xFFFCE0E0), Color(0xFF7F1D1D))
private val CrimsonDark   = accentDark(Color(0xFFFCA5A5), Color(0xFF450A0A), Color(0xFF991B1B), Color(0xFFFEE2E2))

private val RoseLight     = accentLight(Color(0xFFDB2777), Color.White, Color(0xFFFBD5E5), Color(0xFF831843))
private val RoseDark      = accentDark(Color(0xFFF9A8D4), Color(0xFF500724), Color(0xFF9D174D), Color(0xFFFCE7F3))

private val LavenderLight = accentLight(Color(0xFF8B5CF6), Color.White, Color(0xFFEBE3FF), Color(0xFF3B0764))
private val LavenderDark  = accentDark(Color(0xFFC4B5FD), Color(0xFF2E1065), Color(0xFF6D28D9), Color(0xFFEDE9FE))

private val PlumLight     = accentLight(Color(0xFFA21CAF), Color.White, Color(0xFFF6D6F5), Color(0xFF581C5B))
private val PlumDark      = accentDark(Color(0xFFE879F9), Color(0xFF4A044E), Color(0xFF86198F), Color(0xFFFAE8FF))

/**
 * Animate every visible slot of a [ColorScheme] toward [target]. Driving the whole
 * scheme through [animateColorAsState] means theme switches (and system light/dark
 * flips) cross-fade smoothly instead of snapping.
 */
@Composable
private fun animateColorScheme(target: ColorScheme, spec: AnimationSpec<Color>): ColorScheme {
    @Composable
    fun anim(c: Color): Color = animateColorAsState(targetValue = c, animationSpec = spec, label = "scheme-color").value
    return target.copy(
        primary = anim(target.primary),
        onPrimary = anim(target.onPrimary),
        primaryContainer = anim(target.primaryContainer),
        onPrimaryContainer = anim(target.onPrimaryContainer),
        secondary = anim(target.secondary),
        onSecondary = anim(target.onSecondary),
        secondaryContainer = anim(target.secondaryContainer),
        onSecondaryContainer = anim(target.onSecondaryContainer),
        tertiary = anim(target.tertiary),
        onTertiary = anim(target.onTertiary),
        tertiaryContainer = anim(target.tertiaryContainer),
        onTertiaryContainer = anim(target.onTertiaryContainer),
        background = anim(target.background),
        onBackground = anim(target.onBackground),
        surface = anim(target.surface),
        onSurface = anim(target.onSurface),
        surfaceVariant = anim(target.surfaceVariant),
        onSurfaceVariant = anim(target.onSurfaceVariant),
        surfaceTint = anim(target.surfaceTint),
        surfaceContainerLowest = anim(target.surfaceContainerLowest),
        surfaceContainerLow = anim(target.surfaceContainerLow),
        surfaceContainer = anim(target.surfaceContainer),
        surfaceContainerHigh = anim(target.surfaceContainerHigh),
        surfaceContainerHighest = anim(target.surfaceContainerHighest),
        outline = anim(target.outline),
        outlineVariant = anim(target.outlineVariant),
        error = anim(target.error),
        onError = anim(target.onError),
        errorContainer = anim(target.errorContainer),
        onErrorContainer = anim(target.onErrorContainer)
    )
}

// endregion

// region — Extended (non-Material) color tokens --------------------------------

/**
 * Tokens that don't fit Material 3's slots — stress score bands, course tag tints,
 * hairline border colors. Exposed via [LocalLamlaColors] / [MaterialTheme.lamla].
 */
data class LamlaColors(
    val stressChill: Color,
    val stressSteady: Color,
    val stressHeavy: Color,
    val stressCrunch: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    val timelineRail: Color,
    val timelineNow: Color,
    val tabularDigits: Color
) {
    /** Strip semantic colors for the Monochrome theme — everything becomes ink/snow + tonal. */
    fun asMonochrome(): LamlaColors = copy(
        stressChill = hairlineStrong,
        stressSteady = hairlineStrong,
        stressHeavy = hairlineStrong,
        stressCrunch = timelineNow
    )
}

private val LightExtendedColors = LamlaColors(
    stressChill = Palette.Sage,
    stressSteady = Palette.Amber,
    stressHeavy = Color(0xFFC2774C),
    stressCrunch = Palette.Rust,
    hairline = Color(0xFFE5E2DA),
    hairlineStrong = Palette.Stone,
    timelineRail = Palette.Linen,
    timelineNow = Palette.Ink,
    tabularDigits = Palette.Ink
)

private val DarkExtendedColors = LamlaColors(
    stressChill = Palette.SageDark,
    stressSteady = Palette.AmberDark,
    stressHeavy = Color(0xFFE19770),
    stressCrunch = Palette.RustDark,
    hairline = Color(0xFF22222B),
    hairlineStrong = Palette.Iron,
    timelineRail = Palette.Onyx,
    timelineNow = Palette.Snow,
    tabularDigits = Palette.Snow
)

private val GoldExtendedColors = LightExtendedColors.copy(
    timelineNow = Palette.GoldDeep,
    tabularDigits = Palette.Ink
)

internal val LocalLamlaColors = staticCompositionLocalOf { LightExtendedColors }

// endregion

// region — CompositionLocal entrypoint -----------------------------------------

/**
 * Convenience accessor: `MaterialTheme.lamla.colors.stressCrunch`, etc.
 *
 * Keeps domain-specific tokens type-safe and discoverable, without polluting
 * [androidx.compose.material3.ColorScheme] with non-Material slots.
 */
object MaterialThemeExt {
    val colors: LamlaColors @Composable get() = LocalLamlaColors.current
    val spacing: LamlaSpacing @Composable get() = LocalLamlaSpacing.current
    val motion: LamlaMotion @Composable get() = LocalLamlaMotion.current
    val elevation: LamlaElevation @Composable get() = LocalLamlaElevation.current
}

@Suppress("UnusedReceiverParameter")
val MaterialTheme.lamla: MaterialThemeExt get() = MaterialThemeExt

// endregion
