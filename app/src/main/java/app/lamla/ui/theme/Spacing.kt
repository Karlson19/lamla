package app.lamla.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale — 4dp base grid.
 *
 * Most spec'd Android apps default to Material's 16dp gutter; we go a bit more
 * generous on outer padding (20dp) and inside cards (24dp) for a Linear/Things
 * feel. Tight cluster: 4/8/12. Comfortable: 16/20/24. Section breaks: 32/48/64.
 *
 * Named so callers don't have to remember dp values — `spacing.gutter` reads
 * better than `20.dp` and centralizes future tweaks.
 */
data class LamlaSpacing(
    /** 2dp — micro alignment nudges, hairline halos. */
    val hairline: Dp = 2.dp,
    /** 4dp — between tightly-related elements (icon + label). */
    val xxs: Dp = 4.dp,
    /** 8dp — between related elements (chips in a row). */
    val xs: Dp = 8.dp,
    /** 12dp — between items in a list. */
    val sm: Dp = 12.dp,
    /** 16dp — internal card padding (modest). */
    val md: Dp = 16.dp,
    /** 20dp — screen-edge gutter, default content padding. */
    val gutter: Dp = 20.dp,
    /** 24dp — internal card padding (spacious), section header spacing. */
    val lg: Dp = 24.dp,
    /** 32dp — between major sections on a screen. */
    val xl: Dp = 32.dp,
    /** 48dp — top of screen breathing room, between scrolled groupings. */
    val xxl: Dp = 48.dp,
    /** 64dp — empty-state vertical centering. */
    val xxxl: Dp = 64.dp,

    /** Standard touch target — 48dp minimum per accessibility guidelines. */
    val touchTarget: Dp = 48.dp,
    /** Bottom-bar height. */
    val bottomBarHeight: Dp = 64.dp,
    /** Hairline border width (1dp on most devices; can be sub-pixel on hi-dpi). */
    val border: Dp = 1.dp,
    /** Card corner radius — gentle, not pill. */
    val cornerSm: Dp = 8.dp,
    val cornerMd: Dp = 14.dp,
    val cornerLg: Dp = 20.dp,
    val cornerXl: Dp = 28.dp,
    val cornerFull: Dp = 999.dp
)

internal val Spacing = LamlaSpacing()
internal val LocalLamlaSpacing = staticCompositionLocalOf { Spacing }
