package cx.viz.balisurf.data

/**
 * Minimal platform file seam for persisting session logs. A plain interface (not
 * expect/actual) so commonTest can drop in an in-memory fake with zero platform
 * plumbing. Android implements it over filesDir, iOS over the Documents dir.
 */
interface LogFileIo {
    /** Read the whole file as text, or null if it doesn't exist yet. */
    fun readText(): String?

    /** Overwrite the file with [text]. */
    fun writeText(text: String)
}
