package cx.viz.balisurf.scoring

import cx.viz.balisurf.data.SpotCatalog
import cx.viz.balisurf.domain.Conditions
import cx.viz.balisurf.domain.TideState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * These tests ARE the product spec: local surf judgement expressed as pinned
 * assertions. Changing a rule in SpotCatalog should require changing a test —
 * that is the point. No network here; pure scoring.
 */
class SpotScorerTest {

    private val ulu = SpotCatalog.byId("uluwatu")!!
    private val bingin = SpotCatalog.byId("bingin")!!

    /** A clean, in-window SW groundswell hour, wind light offshore. Tide overridable. */
    private fun goodSwell(tide: TideState, tideH: Double = 0.5) = Conditions(
        timeIso = "2026-08-15T07:00",
        swellHeightMeters = 2.0,
        swellDirectionDeg = 220,      // SW, inside every spot's window
        swellPeriodSeconds = 14.0,    // long-period groundswell
        windSpeedKmh = 8.0,           // light
        windDirectionDeg = 110,       // SE = offshore for the Bukit
        tide = tide,
        tideHeightMeters = tideH,
    )

    @Test
    fun uluwatu_scores_well_on_high_tide() {
        val v = SpotScorer.verdict(ulu, listOf(goodSwell(TideState.HIGH)))
        assertTrue(v.stars >= 4, "clean long-period SW swell on Ulu high tide should be 4-5*, got ${v.stars}")
    }

    @Test
    fun uluwatu_is_dead_on_low_tide() {
        // Uluwatu rules exclude LOW — even a perfect swell should gate to 0.
        val v = SpotScorer.verdict(ulu, listOf(goodSwell(TideState.LOW)))
        assertEquals(0, v.stars, "Ulu does not work on low tide per its rules")
    }

    @Test
    fun bingin_needs_low_or_mid_not_high() {
        val onLow = SpotScorer.verdict(bingin, listOf(goodSwell(TideState.LOW)))
        val onHigh = SpotScorer.verdict(bingin, listOf(goodSwell(TideState.HIGH)))
        assertTrue(onLow.stars >= 4, "Bingin should fire on low, got ${onLow.stars}")
        assertEquals(0, onHigh.stars, "Bingin closes out / drowns on high per its rules")
    }

    @Test
    fun wrong_swell_direction_kills_the_score() {
        // N swell (0°) far outside the SW window.
        val northSwell = goodSwell(TideState.HIGH).copy(swellDirectionDeg = 0)
        val v = SpotScorer.verdict(ulu, listOf(northSwell))
        assertTrue(v.stars <= 2, "north swell should score poorly at a SW-facing reef, got ${v.stars}")
    }

    @Test
    fun onshore_gale_blows_it_out() {
        val blownOut = goodSwell(TideState.HIGH).copy(windSpeedKmh = 45.0, windDirectionDeg = 270)
        val v = SpotScorer.verdict(ulu, listOf(blownOut))
        assertEquals(0, v.stars, "35km/h+ over maxWindSpeed should zero the wind gate")
    }

    @Test
    fun too_small_swell_is_flat() {
        val tiny = goodSwell(TideState.HIGH).copy(swellHeightMeters = 0.3)
        val v = SpotScorer.verdict(ulu, listOf(tiny))
        assertEquals(0, v.stars, "below minSwellHeight the spot is flat")
    }

    @Test
    fun short_period_windswell_scores_below_groundswell() {
        val windswell = goodSwell(TideState.HIGH).copy(swellPeriodSeconds = 6.0)
        val ground = goodSwell(TideState.HIGH)
        val vw = SpotScorer.verdict(ulu, listOf(windswell))
        val vg = SpotScorer.verdict(ulu, listOf(ground))
        assertTrue(vw.stars < vg.stars, "6s windswell must score below 14s groundswell")
    }

    @Test
    fun best_window_picks_the_clean_morning_run() {
        // 3 poor hours then 3 clean hours; window should land on the clean run.
        val poor = (5..7).map { goodSwell(TideState.HIGH).copy(timeIso = "2026-08-15T0$it:00", windSpeedKmh = 40.0, windDirectionDeg = 270) }
        val clean = (8..10).map { goodSwell(TideState.HIGH).copy(timeIso = "2026-08-15T${it.toString().padStart(2, '0')}:00") }
        val v = SpotScorer.verdict(ulu, poor + clean)
        assertTrue(v.bestWindow != null, "a clean run exists")
        assertEquals("2026-08-15T08:00", v.bestWindow!!.startIso)
    }

    @Test
    fun compass_conversion_is_correct() {
        assertEquals("N", SpotScorer.compass(0))
        assertEquals("E", SpotScorer.compass(90))
        assertEquals("S", SpotScorer.compass(180))
        assertEquals("SW", SpotScorer.compass(225))
        assertEquals("W", SpotScorer.compass(270))
    }

    @Test
    fun angular_distance_wraps_around_north() {
        // 350° to 10° is 20°, not 340°.
        assertEquals(20.0, SpotScorer.angularDelta(350.0, 10.0), 1e-9)
    }
}
