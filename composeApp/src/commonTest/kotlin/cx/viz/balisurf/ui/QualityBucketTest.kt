package cx.viz.balisurf.ui

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pins the stars→bucket mapping every chip, accent border, and bar chart uses. */
class QualityBucketTest {

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

    @Test
    fun dateParts_indexTheLocalizedWeekdayAndMonthArrays() {
        // 2000-01-01 was a Saturday — a date whose weekday is beyond doubt.
        // Monday-first weekdays => Saturday is 5; January is month 0.
        assertEquals(Triple(5, 1, 0), dateParts("2000-01-01T00:00"))
    }

    @Test
    fun dateParts_swallowGarbage() {
        assertNull(dateParts("not-a-date"))
    }
}
