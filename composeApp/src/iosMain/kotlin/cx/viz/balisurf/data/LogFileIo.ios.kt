package cx.viz.balisurf.data

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/** iOS session-log file, stored in the app's Documents directory. */
@OptIn(ExperimentalForeignApi::class)
class IosLogFileIo : LogFileIo {
    private val path: String = run {
        val docs = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ).first() as String
        "$docs/session_logs.json"
    }

    override fun readText(): String? =
        NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)

    override fun writeText(text: String) {
        (text as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }
}
