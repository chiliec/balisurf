package cx.viz.balisurf.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** "Tropic Clean" palette — the app's only source of color constants. */
object BaliColors {
    val Teal = Color(0xFF0D9488)
    val DeepTeal = Color(0xFF0B5C6B)
    val Background = Color(0xFFF7FAFB)
    val Ink = Color(0xFF12333F)
    val Gray = Color(0xFFBDBDBD)
    val Coral = Color(0xFFEF6C57)
    val Amber = Color(0xFFF59E0B)
    /** Tinted fill for condition tiles inside white cards. */
    val TileTint = Color(0xFFF0F7F7)
    /** Tinted container for the session-log card + tide pills. */
    val CardTint = Color(0xFFE6F2F4)
}

private val TropicLight = lightColorScheme(
    primary = BaliColors.Teal,
    onPrimary = Color.White,
    secondary = BaliColors.DeepTeal,
    onSecondary = Color.White,
    background = BaliColors.Background,
    onBackground = BaliColors.Ink,
    surface = Color.White,
    onSurface = BaliColors.Ink,
)

@Composable
fun BaliSurfTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = TropicLight, content = content)

/** Verdict bucket: one shared label+color mapping for chips, card accents, bars. */
data class QualityBucket(val label: String, val container: Color, val content: Color)

fun qualityBucket(stars: Int): QualityBucket = when {
    stars <= 0 -> QualityBucket("FLAT", BaliColors.Gray, BaliColors.Ink)
    stars <= 2 -> QualityBucket("POOR", BaliColors.Coral, Color.White)
    stars == 3 -> QualityBucket("FAIR", BaliColors.Amber, Color.White)
    else -> QualityBucket("GO", BaliColors.Teal, Color.White)
}

/** Bar-chart shading: light→dark teal ramp by stars (finer than the 4 buckets). */
fun barShade(stars: Int): Color = when (stars) {
    0 -> Color(0xFFE3EBEA)
    1 -> Color(0xFFCBE8E5)
    2 -> Color(0xFF99D5CF)
    3 -> Color(0xFF2DD4BF)
    4 -> BaliColors.Teal
    else -> BaliColors.DeepTeal
}
