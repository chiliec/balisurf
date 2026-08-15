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
    fun coordinates_are_within_the_region() {
        // Bali + Nusa islands + Lombok bounding box, generous margin. Catches a
        // swapped lat/lon or a typo without rejecting the Lombok spots.
        SpotCatalog.spots.forEach { s ->
            assertTrue(s.latitude in -9.1..-8.0, "${s.id} latitude off-region: ${s.latitude}")
            assertTrue(s.longitude in 114.3..116.6, "${s.id} longitude off-region: ${s.longitude}")
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

    @Test
    fun covers_lembongan_and_lombok() {
        listOf("shipwrecks", "playgrounds", "desertpoint", "gerupuk", "mawi").forEach {
            assertTrue(SpotCatalog.byId(it) != null, "missing expansion spot $it")
        }
    }

    @Test
    fun wraparound_offshore_windows_are_north_crossing() {
        // Gerupuk/Mawi use an offshore window that crosses north (min > max, e.g.
        // 315..45). The scorer's circular windowScore handles this; this guards
        // the intent so a "fix" that swaps them to 45..315 (the whole compass
        // minus north) gets caught.
        listOf("gerupuk", "mawi").forEach { id ->
            val r = SpotCatalog.byId(id)!!.rules
            assertTrue(r.offshoreWindMin > r.offshoreWindMax, "$id should be a north-crossing window")
        }
    }
}
