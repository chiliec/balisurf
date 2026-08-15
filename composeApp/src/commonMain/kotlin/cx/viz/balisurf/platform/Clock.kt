package cx.viz.balisurf.platform

/**
 * Current local wall-clock time as "yyyy-MM-ddTHH:mm". A tiny expect/actual seam
 * (java.time on Android, NSDate on iOS) — avoids depending on kotlinx.datetime's
 * Clock in commonMain, whose `Clock.System` moved under Kotlin 2.4 and fails the
 * native compile. Mirrors the sibling apps' Clock expect/actual pattern.
 */
expect fun nowIso(): String
