package my.naqibrafiqi.pustaka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import my.naqibrafiqi.pustaka.ui.theme.AppearanceViewModel
import my.naqibrafiqi.pustaka.ui.theme.PustakaAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appearanceViewModel: AppearanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
            // Waits for the settings (a few milliseconds), so the app doesn't flash in the wrong theme.
            val chosen = appearance ?: return@setContent
            PustakaAppTheme(chosen) {
                PustakaRoot()
            }
        }
    }
}
