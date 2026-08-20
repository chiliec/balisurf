package cx.viz.balisurf.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.viz.balisurf.domain.Conditions
import cx.viz.balisurf.domain.Spot
import cx.viz.balisurf.scoring.SpotScorer

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

/** Verdict chip: "4★ GO" pill in the bucket's colors. */
@Composable
fun VerdictChip(stars: Int, modifier: Modifier = Modifier) {
    val b = qualityBucket(stars)
    Text(
        "$stars★ ${b.label}",
        modifier = modifier
            .background(b.container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        color = b.content,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

/** Uppercase letter-spaced micro-label for region headers and card sections. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) = Text(
    text.uppercase(),
    modifier = modifier,
    color = BaliColors.DeepTeal.copy(alpha = 0.75f),
    style = MaterialTheme.typography.labelSmall,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = 1.4.sp,
)

/**
 * 24h score bars, shared by list cards (24.dp) and the detail chart (80.dp).
 * Zero-score hours keep a 2dp sliver so the strip still reads as a timeline.
 */
@Composable
fun HourBars(spot: Spot, hours: List<Conditions>, height: Dp) {
    val scores = hours.map { SpotScorer.scoreHour(spot, it) }
    if (scores.isEmpty()) return
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val n = scores.size
        val gap = 2f
        val barW = (size.width - gap * (n - 1)) / n
        val floor = 2.dp.toPx()
        scores.forEachIndexed { i, s ->
            val h = maxOf(s.toFloat().coerceIn(0f, 1f) * size.height, floor)
            drawRect(
                color = barShade(SpotScorer.toStars(s)),
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
            )
        }
    }
}
