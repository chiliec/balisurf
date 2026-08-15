package cx.viz.balisurf.data

import cx.viz.balisurf.domain.TideState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Structural guards on the catalog. These don't judge surf quality (that's the
 * SpotScorer tests + real session data) — they catch data-entry mistakes: dup
 * ids, out-of-range angles, empty windows, spots off the Bali map.
 */
class SpotCatalogTest {

    @Test
    fun ids_are_unique() {
        val ids = SpotCatalog.spots.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate spot id in catalog")
    }

    @Test
    fun byId_round_trips_every_spot() {
        SpotCatalog.spots.forEach { s ->
            assertEquals(s, SpotCatalog.byId(s.id), "byId failed for ${s.id}")
        }
    }

    @Test
    fun coordinates_are_within_bali() {
        // Bali bounding box, generous margin. Catches a swapped lat/lon or typo.
        SpotCatalog.spots.forEach { s ->
            assertTrue(s.latitude in -8.95..-8.05, "${s.id} latitude off Bali: ${s.latitude}")
            assertTrue(s.longitude in 114.4..115.75, "${s.id} longitude off Bali: ${s.longitude}")
        }
    }

    @Test
    fun rule_windows_are_valid() {
        SpotCatalog.spots.forEach { s ->
            val r = s.rules
            assertTrue(r.worksOnTide.isNotEmpty(), "${s.id} works on no tide")
            assertTrue(r.swellDirectionMin in 0..360 && r.swellDirectionMax in 0..360, "${s.id} swell dir out of range")
            assertTrue(r.offshoreWindMin in 0..360 && r.offshoreWindMax in 0..360, "${s.id} wind dir out of range")
            assertTrue(r.minPeriodSeconds > 0, "${s.id} non-positive min period")
            assertTrue(r.minSwellHeightMeters > 0, "${s.id} non-positive min swell height")
            assertTrue(r.maxWindSpeedKmh > 0, "${s.id} non-positive max wind speed")
        }
    }

    @Test
    fun east_coast_spots_want_west_offshore() {
        // Keramas/Nusa Dua/Serangan face east: offshore must be a westerly window,
        // not the Bukit's easterly trade. Guards against copy-paste of a Bukit rule.
        listOf("keramas", "nusadua", "serangan").forEach { id ->
            val r = SpotCatalog.byId(id)!!.rules
            assertTrue(r.offshoreWindMin >= 200, "$id offshore window should be westerly, got ${r.offshoreWindMin}")
        }
    }

    @Test
    fun catalog_grew_beyond_the_original_bukit_five() {
        assertTrue(SpotCatalog.spots.size >= 10, "expected the expanded catalog")
        // The original five must still be present.
        listOf("uluwatu", "padang", "bingin", "impossibles", "dreamland").forEach {
            assertTrue(SpotCatalog.byId(it) != null, "missing original spot $it")
        }
    }
}
