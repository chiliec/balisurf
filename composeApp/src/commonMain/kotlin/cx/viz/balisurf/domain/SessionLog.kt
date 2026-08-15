package cx.viz.balisurf.domain

import kotlinx.serialization.Serializable

/**
 * A user's report that a spot did or didn't work, captured WITH the conditions at
 * that moment. This is simultaneously the app's growth loop (users contribute
 * knowledge) and the ground-truth pipeline: enough logs let us calibrate each
 * spot's rules — and eventually its reef depth — from real sessions instead of
 * guesses. On-device only in v0.1 (no backend); exportable for analysis.
 *
 * The conditions snapshot is what makes a log calibration-grade: "worked" is
 * meaningless without the swell/tide/wind it worked in.
 */
@Serializable
data class SessionLog(
    val spotId: String,
    /** ISO-ish timestamp the log was created (device local), e.g. 2026-08-15T07:30. */
    val timestamp: String,
    /** The user's call: did the spot work? */
    val worked: Boolean,
    /** Optional free-text note ("too crowded", "perfect on the push"). */
    val note: String = "",
    // --- conditions snapshot at log time (nullable: user may log without live data) ---
    val swellHeightMeters: Double? = null,
    val swellDirectionDeg: Int? = null,
    val swellPeriodSeconds: Double? = null,
    val windSpeedKmh: Double? = null,
    val windDirectionDeg: Int? = null,
    val tide: String? = null,
    val tideHeightMeters: Double? = null,
)
