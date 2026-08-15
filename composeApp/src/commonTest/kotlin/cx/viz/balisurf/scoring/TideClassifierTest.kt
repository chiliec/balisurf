package cx.viz.balisurf.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class TideClassifierTest {

    @Test
    fun bands_a_semidiurnal_day_into_low_mid_high() {
        // Synthetic tide range -0.9 .. 1.5 (like the real Uluwatu MSL series).
        val heights = listOf(-0.9, -0.3, 0.3, 0.9, 1.5, 0.9, 0.3, -0.3)
        val states = TideClassifier.classify(heights)
        assertEquals(TideState.LOW, states.first())   // -0.9 = bottom third
        assertEquals(TideState.HIGH, states[4])        // 1.5 = top third
    }

    @Test
    fun flat_series_is_all_mid() {
        val states = TideClassifier.classify(listOf(0.5, 0.5, 0.5))
        assertEquals(listOf(TideState.MID, TideState.MID, TideState.MID), states)
    }

    @Test
    fun classify_one_matches_the_series_banding() {
        assertEquals(TideState.LOW, TideClassifier.classifyOne(-0.8, dayMin = -0.9, dayMax = 1.5))
        assertEquals(TideState.HIGH, TideClassifier.classifyOne(1.4, dayMin = -0.9, dayMax = 1.5))
        assertEquals(TideState.MID, TideClassifier.classifyOne(0.3, dayMin = -0.9, dayMax = 1.5))
    }
}
