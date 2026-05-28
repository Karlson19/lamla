package app.lamla.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color tokens.
 *
 * Not Material You default purple. Restrained, warm-neutral chrome with a single
 * brand accent. Per-course color comes from the course's own [Course.color] —
 * the app chrome stays out of the way.
 *
 * Light surface: warm off-white #FAFAF7 (not stark #FFFFFF)
 * Dark surface:  cool ink     #0B0B0E (not pure #000000)
 *
 * Surfaces step in luminance by ~3-4% per level. No heavy shadows; depth comes
 * from these subtle tints + 1dp hairline borders ([CardBorder] tokens).
 */
internal object Palette {

    // -- Neutral light scale (warm) --------------------------------------------
    val Ivory       = Color(0xFFFAFAF7) // base surface
    val Bone        = Color(0xFFF3F2EE) // raised surface 1
    val Linen       = Color(0xFFEAE8E1) // raised surface 2 / divider tint
    val Stone       = Color(0xFFD7D4CB) // hairline border
    val Slate       = Color(0xFF8E8B84) // muted text
    val Graphite    = Color(0xFF4A4843) // secondary text
    val Ink         = Color(0xFF1A1A1A) // primary text on light

    // -- Neutral dark scale (cool, slight blue undertone) ----------------------
    val Obsidian    = Color(0xFF0B0B0E) // base surface
    val Charcoal    = Color(0xFF131318) // raised surface 1
    val Onyx        = Color(0xFF1B1B22) // raised surface 2
    val Iron        = Color(0xFF2A2A33) // hairline border (dark)
    val Ash         = Color(0xFF7E7E89) // muted text (dark)
    val Mist        = Color(0xFFC9C9D2) // secondary text (dark)
    val Snow        = Color(0xFFF2F2F4) // primary text on dark

    // -- Accent (KNUST gold, single brand color) -------------------------------
    val Gold        = Color(0xFFC5A35A)
    val GoldDeep    = Color(0xFFA4863F) // darker variant for active states
    val GoldSoft    = Color(0xFFE3D4A8) // pressed/container on light
    val GoldGhost   = Color(0xFF3A3220) // container on dark (low-chroma)

    // -- Semantic --------------------------------------------------------------
    // Earthy, restrained — not bright web red/green/yellow. Closer to Linear/Things 3.
    val Sage        = Color(0xFF5C8064) // chill / success
    val SageDark    = Color(0xFF7BAA85)
    val Amber       = Color(0xFFB58532) // busy
    val AmberDark   = Color(0xFFD9A95A)
    val Rust        = Color(0xFFB05A47) // crunch / error (not pure red)
    val RustDark    = Color(0xFFCE7A65)

    // -- Per-course palette (12 carefully chosen, all desaturated) -------------
    // Used for course color picker. Avoid pure primaries — these feel curated.
    val CoursePalette: List<Color> = listOf(
        Color(0xFFB85C5C), // brick
        Color(0xFFC07F3D), // ochre
        Color(0xFFA68B3A), // mustard
        Color(0xFF6E8B47), // moss
        Color(0xFF4A8071), // teal
        Color(0xFF4F7396), // dusty blue
        Color(0xFF6E6BA8), // periwinkle
        Color(0xFF8E5DA0), // plum
        Color(0xFFB46A8C), // dusty rose
        Color(0xFF7A6754), // walnut
        Color(0xFF566270), // slate blue
        Color(0xFF8C8580)  // warm grey
    )
}
