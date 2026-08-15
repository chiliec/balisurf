package cx.viz.balisurf.data

import cx.viz.balisurf.domain.Conditions
import cx.viz.balisurf.domain.SessionLog
import kotlinx.serialization.json.Json

/**
 * On-device store for session logs. Persists the whole list as a JSON array via
 * the [LogFileIo] seam (no DB needed at v0.1 scale). Pure logic lives here in
 * commonMain and is unit-tested with an in-memory fake IO.
 *
 * `exportCsv()` emits exactly the shape `tools/sdb/calibrate.py --csv` and the
 * rules-calibration workflow consume, closing the loop from a user's tap to
 * ground-truth data.
 */
class SessionLogStore(private val io: LogFileIo) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private var cache: MutableList<SessionLog>? = null

    fun all(): List<SessionLog> = load().toList()

    fun forSpot(spotId: String): List<SessionLog> = load().filter { it.spotId == spotId }

    fun countForSpot(spotId: String): Int = load().count { it.spotId == spotId }

    /** Append a log and persist. Returns the new total for the spot. */
    fun add(log: SessionLog): Int {
        val list = load()
        list.add(log)
        persist(list)
        return list.count { it.spotId == log.spotId }
    }

    /** Build a log from a spot id, worked flag, timestamp, and optional live conditions. */
    fun logSession(
        spotId: String,
        worked: Boolean,
        timestamp: String,
        note: String = "",
        conditions: Conditions? = null,
    ): Int = add(
        SessionLog(
            spotId = spotId,
            timestamp = timestamp,
            worked = worked,
            note = note,
            swellHeightMeters = conditions?.swellHeightMeters,
            swellDirectionDeg = conditions?.swellDirectionDeg,
            swellPeriodSeconds = conditions?.swellPeriodSeconds,
            windSpeedKmh = conditions?.windSpeedKmh,
            windDirectionDeg = conditions?.windDirectionDeg,
            tide = conditions?.tide?.name,
            tideHeightMeters = conditions?.tideHeightMeters,
        )
    )

    /**
     * Export all logs as CSV. Columns match the calibration/analysis workflow:
     * spot,timestamp,worked,swell_m,swell_dir,swell_period,wind_kmh,wind_dir,
     * tide,tide_m,note. Blank cells for missing values.
     */
    fun exportCsv(): String {
        val header = "spot,timestamp,worked,swell_m,swell_dir,swell_period," +
            "wind_kmh,wind_dir,tide,tide_m,note"
        val rows = load().joinToString("\n") { l ->
            listOf(
                l.spotId,
                l.timestamp,
                if (l.worked) "1" else "0",
                l.swellHeightMeters?.toString() ?: "",
                l.swellDirectionDeg?.toString() ?: "",
                l.swellPeriodSeconds?.toString() ?: "",
                l.windSpeedKmh?.toString() ?: "",
                l.windDirectionDeg?.toString() ?: "",
                l.tide ?: "",
                l.tideHeightMeters?.toString() ?: "",
                csvEscape(l.note),
            ).joinToString(",")
        }
        return if (rows.isEmpty()) header else "$header\n$rows"
    }

    // --- internals ---

    private fun load(): MutableList<SessionLog> {
        cache?.let { return it }
        val text = io.readText()
        val list = if (text.isNullOrBlank()) {
            mutableListOf()
        } else {
            try {
                json.decodeFromString<List<SessionLog>>(text).toMutableList()
            } catch (e: Exception) {
                // Corrupt/old file: start fresh rather than crash. Logs are
                // best-effort contributions, never load-bearing app state.
                mutableListOf()
            }
        }
        cache = list
        return list
    }

    private fun persist(list: MutableList<SessionLog>) {
        cache = list
        io.writeText(json.encodeToString(list.toList()))
    }

    private fun csvEscape(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s
}
