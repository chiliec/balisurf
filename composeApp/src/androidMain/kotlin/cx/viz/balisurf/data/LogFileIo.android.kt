package cx.viz.balisurf.data

import android.content.Context
import java.io.File

/** Android session-log file, stored in the app's private filesDir. */
class AndroidLogFileIo(context: Context) : LogFileIo {
    private val file = File(context.filesDir, "session_logs.json")

    override fun readText(): String? =
        if (file.exists()) file.readText() else null

    override fun writeText(text: String) {
        file.writeText(text)
    }
}
