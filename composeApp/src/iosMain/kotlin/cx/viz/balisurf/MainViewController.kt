package cx.viz.balisurf

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import cx.viz.balisurf.data.IosLogFileIo
import cx.viz.balisurf.data.OpenMeteoForecastSource
import cx.viz.balisurf.data.SessionLogStore
import cx.viz.balisurf.ui.App
import cx.viz.balisurf.ui.AppModule

fun MainViewController() = ComposeUIViewController {
    val module = remember {
        AppModule(
            forecast = OpenMeteoForecastSource(),
            logs = SessionLogStore(IosLogFileIo()),
        )
    }
    App(module)
}
