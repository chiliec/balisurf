package cx.viz.balisurf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cx.viz.balisurf.data.AndroidLogFileIo
import cx.viz.balisurf.data.OpenMeteoForecastSource
import cx.viz.balisurf.data.SessionLogStore
import cx.viz.balisurf.ui.App
import cx.viz.balisurf.ui.AppModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val module = AppModule(
            forecast = OpenMeteoForecastSource(),
            logs = SessionLogStore(AndroidLogFileIo(applicationContext)),
        )
        setContent { App(module) }
    }
}
