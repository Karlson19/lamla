package app.lamla.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.lamla.R

/**
 * Typography.
 *
 * Inter for everything (UI), JetBrains Mono for the Pomodoro timer face only
 * (because monospace digits don't shimmy as the seconds tick). Both via the
 * downloadable-fonts provider — no font files bundled, no APK bloat.
 *
 * The type scale is restrained: 4 display sizes, 3 body sizes. Tracking widens
 * at small sizes (UI labels) and tightens at display sizes. Line-height-style
 * is centered + trimmed so cap/baseline metrics don't add unwanted padding
 * around headlines (Compose's default leaves big gaps at the top of headings).
 *
 * Numeric features: tabular figures everywhere — countdowns, durations, timer.
 * Achieved via [TextStyle.fontFeatureSettings] = "tnum".
 */

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val Inter = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Bold)
)

private val Mono = FontFamily(
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Medium)
)

val MonoFamily: FontFamily get() = Mono

private val centeredAndTrimmed = LineHeightStyle(
    alignment = Alignment.Center,
    trim = Trim.Both
)

// Feature flags for OpenType — applied via fontFeatureSettings.
private const val TabularNums = "\"tnum\" 1, \"lnum\" 1"
private const val CaseSensitive = "\"case\" 1, \"tnum\" 1"

internal val LamlaTypography = Typography(
    // Display — used sparingly (stress score number, countdown).
    // Light weight at large sizes feels editorial. Tight tracking.
    displayLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Light,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.02).em,
        lineHeightStyle = centeredAndTrimmed,
        fontFeatureSettings = TabularNums
    ),
    displayMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Light,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.018).em,
        lineHeightStyle = centeredAndTrimmed,
        fontFeatureSettings = TabularNums
    ),
    displaySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Light,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.015).em,
        lineHeightStyle = centeredAndTrimmed,
        fontFeatureSettings = TabularNums
    ),

    // Headline — section headers.
    headlineLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.012).em,
        lineHeightStyle = centeredAndTrimmed
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
        lineHeightStyle = centeredAndTrimmed
    ),
    headlineSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.005).em,
        lineHeightStyle = centeredAndTrimmed
    ),

    // Title — card titles, dialog titles.
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

    // Body — long-form content.
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

    // Label — chips, button text, table column headers. Slightly wider tracking
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
