package cx.viz.balisurf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cx.viz.balisurf.data.OpenMeteoForecastSource
import cx.viz.balisurf.ui.App
import cx.viz.balisurf.ui.AppModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val module = AppModule(
            forecast = OpenMeteoForecastSource(),
        )
        setContent { App(module) }
    }
}
