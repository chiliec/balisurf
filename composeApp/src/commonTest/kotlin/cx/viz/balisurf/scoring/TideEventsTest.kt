package cx.viz.balisurf.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TideEventsTest {

    // A clean semidiurnal shape: low, rising to high, falling to low.
    private val times = (0..8).map { "2026-08-15T0$it:00" }
    private val heights = listOf(-0.9, -0.5, 0.2, 0.9, 1.4, 0.9, 0.2, -0.5, -0.9)

    @Test
    fun detects_the_high_and_the_lows() {
        val events = TideEvents.detect(times, heights)
        // Interior extrema only (endpoints can't be classified): one HIGH at the peak.
        val highs = events.filter { it.kind == TideEvent.Kind.HIGH }
        assertEquals(1, highs.size)
        assertEquals("2026-08-15T04:00", highs.first().timeIso)
        assertEquals(1.4, highs.first().heightMeters, 1e-9)
    }

    @Test
    fun vertex_offset_is_zero_for_symmetric_peak() {
        // Symmetric around the middle -> vertex sits exactly on the sample.
        assertEquals(0.0, TideEvents.vertexOffset(1.0, 2.0, 1.0), 1e-9)
    }

    @Test
    fun vertex_offset_leans_toward_the_higher_neighbour() {
        // Right neighbour higher -> vertex shifts positive (toward the later sample).
        assertTrue(TideEvents.vertexOffset(0.0, 2.0, 1.0) > 0.0)
        assertTrue(TideEvents.vertexOffset(1.0, 2.0, 0.0) < 0.0)
    }

    @Test
    fun flat_series_yields_no_events() {
        val flat = List(5) { 0.5 }
        assertTrue(TideEvents.detect((0..4).map { "t$it" }, flat).isEmpty())
    }

    @Test
    fun mismatched_or_short_input_is_safe() {
        assertTrue(TideEvents.detect(listOf("a"), listOf(1.0, 2.0)).isEmpty())
        assertTrue(TideEvents.detect(listOf("a", "b"), listOf(1.0, 2.0)).isEmpty())
    }
}
