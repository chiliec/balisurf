package cx.viz.balisurf.data

import cx.viz.balisurf.domain.Conditions
import cx.viz.balisurf.domain.TideState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** In-memory LogFileIo so the store logic is tested with no platform plumbing. */
private class FakeLogFileIo(var text: String? = null) : LogFileIo {
    override fun readText(): String? = text
    override fun writeText(text: String) { this.text = text }
}

class SessionLogStoreTest {

    private fun cond() = Conditions(
        timeIso = "2026-08-15T07:00",
        swellHeightMeters = 2.0, swellDirectionDeg = 220, swellPeriodSeconds = 14.0,
        windSpeedKmh = 8.0, windDirectionDeg = 110,
        tide = TideState.LOW, tideHeightMeters = -0.5,
    )

    @Test
    fun add_persists_and_counts_per_spot() {
        val io = FakeLogFileIo()
        val store = SessionLogStore(io)
        store.logSession("uluwatu", worked = true, timestamp = "2026-08-15T07:00")
        store.logSession("uluwatu", worked = false, timestamp = "2026-08-15T09:00")
        store.logSession("bingin", worked = true, timestamp = "2026-08-15T07:00")
        assertEquals(2, store.countForSpot("uluwatu"))
        assertEquals(1, store.countForSpot("bingin"))
        assertEquals(3, store.all().size)
        assertTrue(io.text!!.contains("uluwatu"))
    }

    @Test
    fun persistence_survives_a_new_store_over_same_io() {
        val io = FakeLogFileIo()
        SessionLogStore(io).logSession("padang", true, "2026-08-15T06:00")
        // A fresh store reading the same backing file must see the log.
        val reopened = SessionLogStore(io)
        assertEquals(1, reopened.countForSpot("padang"))
    }

    @Test
    fun corrupt_file_starts_fresh_not_crash() {
        val io = FakeLogFileIo("{ this is not valid json ]")
        val store = SessionLogStore(io)
        assertEquals(0, store.all().size)
        store.logSession("uluwatu", true, "2026-08-15T07:00")
        assertEquals(1, store.countForSpot("uluwatu"))
    }

    @Test
    fun export_csv_has_header_and_snapshot_columns() {
        val io = FakeLogFileIo()
        val store = SessionLogStore(io)
        store.logSession("bingin", worked = true, timestamp = "2026-08-15T07:00", conditions = cond())
        val csv = store.exportCsv()
        val lines = csv.split("\n")
        assertEquals(
            "spot,timestamp,worked,swell_m,swell_dir,swell_period,wind_kmh,wind_dir,tide,tide_m,note",
            lines[0],
        )
        // row: spot, ts, worked=1, swell 2.0, dir 220, period 14.0 ... tide LOW
        assertTrue(lines[1].startsWith("bingin,2026-08-15T07:00,1,2.0,220,14.0,"))
        assertTrue(lines[1].contains("LOW"))
    }

    @Test
    fun export_csv_escapes_commas_in_notes() {
        val io = FakeLogFileIo()
        val store = SessionLogStore(io)
        store.logSession("uluwatu", true, "2026-08-15T07:00", note = "clean, glassy")
        val csv = store.exportCsv()
        assertTrue(csv.contains("\"clean, glassy\""))
    }

    @Test
    fun empty_store_export_is_header_only() {
        val store = SessionLogStore(FakeLogFileIo())
        assertTrue(store.exportCsv().startsWith("spot,timestamp,worked"))
        assertEquals(1, store.exportCsv().split("\n").size)
    }
}
