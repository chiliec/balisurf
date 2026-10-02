package cx.viz.balisurf.platform

import android.os.Build
import cx.viz.balisurf.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

internal actual val userAgent: String =
    "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) BaliSurf/${BuildConfig.VERSION_NAME}"

internal actual fun postJson(url: String, body: String, userAgent: String) {
    Thread {
        runCatching {
            (URL(url).openConnection() as HttpURLConnection).run {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = 5_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", userAgent)
                outputStream.use { it.write(body.toByteArray()) }
                responseCode
                disconnect()
            }
        }
    }.start()
}
