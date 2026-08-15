package cx.viz.balisurf.platform

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

actual fun nowIso(): String = LocalDateTime.now().format(fmt)
