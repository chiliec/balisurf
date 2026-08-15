package cx.viz.balisurf.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDate

/** iOS local timestamp as "yyyy-MM-ddTHH:mm" via NSDateFormatter. */
@OptIn(ExperimentalForeignApi::class)
actual fun nowIso(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd'T'HH:mm"
    return formatter.stringFromDate(NSDate())
}
