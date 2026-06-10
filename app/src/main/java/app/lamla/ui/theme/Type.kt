@file:OptIn(ExperimentalTextApi::class)

package app.lamla.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.lamla.R

/**
 * Typography.
 *
 * Two voices, deliberately paired (the editorial convention):
 *   - **Fraunces** (a warm old-style serif) owns the display + headline slots -
 *     the greeting, screen mastheads, hero numbers. It's what makes Lamla read
 *     like a well-set magazine instead of another sans-serif utility app.
 *   - **Inter** stays on title/body/label - the workhorse UI voice. Crisp at
 *     small sizes, never competes with the serif above it.
 *   - **JetBrains Mono** remains the Pomodoro timer face only (monospace digits
 *     don't shimmy as the seconds tick).
 *
 * All three ship **bundled as variable fonts** (res/font). The first release
 * fetched them through Play Services' downloadable-fonts at runtime - which
 * meant no Play Services, a cold cache, or a slow network silently dropped the
 * entire app to Roboto and the design never rendered. ~1.4MB of APK buys
 * deterministic typography on every device, offline, first frame.
 *
 * Fraunces is instantiated at two optical sizes, like a real foundry family:
 * a high-contrast **Display** cut (opsz 84) for the big serif moments and a
 * sturdier **Text** cut (opsz 40) for headlines, so small serif text never
 * goes spindly and big serif text never looks blunt.
 *
 * The type scale is restrained: 3 display sizes, 3 headline, 3 body. Tracking
 * widens at small sizes (UI labels) and tightens at display sizes.
 * Line-height-style is centered + trimmed so cap/baseline metrics don't add
 * unwanted padding around headlines.
 *
 * Numeric features: tabular figures everywhere - countdowns, durations, timer.
 * Achieved via [TextStyle.fontFeatureSettings] = "tnum".
 */

private fun interAt(weight: Int) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

private val Inter = FontFamily(
    interAt(300), interAt(400), interAt(500), interAt(600), interAt(700)
)

private fun frauncesAt(weight: Int, opticalSize: Float) = Font(
    resId = R.font.fraunces_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opticalSize)
    )
)

/** The display cut: refined, high-contrast strokes for 30sp+. */
private val FrauncesDisplay = FontFamily(
    frauncesAt(300, 84f), frauncesAt(400, 84f), frauncesAt(500, 84f), frauncesAt(600, 84f)
)

/** The text cut: sturdier strokes that hold up at headline sizes. */
private val FrauncesText = FontFamily(
    frauncesAt(400, 40f), frauncesAt(500, 40f), frauncesAt(600, 40f), frauncesAt(700, 40f)
)

private fun monoAt(weight: Int) = Font(
    resId = R.font.jetbrains_mono_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

private val Mono = FontFamily(monoAt(300), monoAt(400), monoAt(500))

val MonoFamily: FontFamily get() = Mono
val SerifFamily: FontFamily get() = FrauncesDisplay

private val centeredAndTrimmed = LineHeightStyle(
    alignment = Alignment.Center,
    trim = Trim.Both
)

// Feature flags for OpenType - applied via fontFeatureSettings.
private const val TabularNums = "\"tnum\" 1, \"lnum\" 1"
private const val CaseSensitive = "\"case\" 1, \"tnum\" 1"

internal val LamlaTypography = Typography(
    // Display - used sparingly (greeting, stress score, hero CWA). The serif at
    // a calm Regular weight is the signature: editorial, classic, unmistakably ours.
    displayLarge = TextStyle(
        fontFamily = FrauncesDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 54.sp,
        lineHeight = 58.sp,
        letterSpacing = (-0.015).em,
        lineHeightStyle = centeredAndTrimmed,
        fontFeatureSettings = TabularNums
    ),
    displayMedium = TextStyle(
        fontFamily = FrauncesDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.012).em,
        lineHeightStyle = centeredAndTrimmed,
        fontFeatureSettings = TabularNums
    ),
    displaySmall = TextStyle(
        fontFamily = FrauncesDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.01).em,
        lineHeightStyle = centeredAndTrimmed,
        fontFeatureSettings = TabularNums
    ),

    // Headline - screen mastheads and section headers. The sturdier text cut:
    // enough ink to anchor a page without shouting.
    headlineLarge = TextStyle(
        fontFamily = FrauncesText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.008).em,
        lineHeightStyle = centeredAndTrimmed
    ),
    headlineMedium = TextStyle(
        fontFamily = FrauncesText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.006).em,
        lineHeightStyle = centeredAndTrimmed
    ),
    headlineSmall = TextStyle(
        fontFamily = FrauncesText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.003).em,
        lineHeightStyle = centeredAndTrimmed
    ),

    // Title - card titles, dialog titles.
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.003).em,
        lineHeightStyle = centeredAndTrimmed
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.em,
        lineHeightStyle = centeredAndTrimmed
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.005.em,
        lineHeightStyle = centeredAndTrimmed
    ),

    // Body - long-form content.
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.em,
        lineHeightStyle = centeredAndTrimmed
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.005.em,
        lineHeightStyle = centeredAndTrimmed
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.01.em,
        lineHeightStyle = centeredAndTrimmed
    ),

    // Label - chips, button text, table column headers. Slightly wider tracking
    // at small sizes for legibility (Pentagram convention).
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.01.em,
        lineHeightStyle = centeredAndTrimmed
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.em,
        lineHeightStyle = centeredAndTrimmed,
        fontFeatureSettings = CaseSensitive
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.04.em,
        lineHeightStyle = centeredAndTrimmed,
        fontFeatureSettings = CaseSensitive
    )
)

/** Special-purpose styles outside the Material slots. */
object LamlaTextStyles {
    /** Countdown / Pomodoro display. Monospaced so digits don't shift. */
    val Timer = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-0.015).em,
        lineHeightStyle = centeredAndTrimmed,
        fontFeatureSettings = TabularNums
    )

    /** Section ALL-CAPS headers (Linear-style). */
    val SectionLabel = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.08.em,
        fontFeatureSettings = CaseSensitive
    )

    /** Numeric metadata (e.g. weight %, hours left). Tabular figures, slightly muted weight. */
    val Metric = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.01.em,
        fontFeatureSettings = TabularNums
    )
}
