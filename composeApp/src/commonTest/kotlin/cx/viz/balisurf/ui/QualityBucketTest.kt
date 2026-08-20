package cx.viz.balisurf.ui

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pins the stars→bucket mapping every chip, accent border, and bar chart uses. */
class QualityBucketTest {

    @Test
    fun bucketsMapStarsToLabels() {
        assertEquals("FLAT", qualityBucket(0).label)
        assertEquals("POOR", qualityBucket(1).label)
        assertEquals("POOR", qualityBucket(2).label)
        assertEquals("FAIR", qualityBucket(3).label)
        assertEquals("GO", qualityBucket(4).label)
        assertEquals("GO", qualityBucket(5).label)
    }

    @Test
    fun bucketColorsComeFromThePalette() {
        assertEquals(BaliColors.Gray, qualityBucket(0).container)
        assertEquals(BaliColors.Coral, qualityBucket(2).container)
        assertEquals(BaliColors.Amber, qualityBucket(3).container)
        assertEquals(BaliColors.Teal, qualityBucket(5).container)
        assertEquals(Color.White, qualityBucket(5).content)
    }

    @Test
    fun barShadesAreDistinctPerStar() {
        val shades = (0..5).map { barShade(it) }
        assertEquals(6, shades.toSet().size)
        assertEquals(BaliColors.Teal, barShade(4))
    }
}
