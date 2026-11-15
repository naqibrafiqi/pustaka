package my.naqibrafiqi.pustaka.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import my.naqibrafiqi.pustaka.data.settings.AppTheme
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import javax.inject.Inject

/** How the app looks, from Settings → Appearance. */
data class Appearance(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = false,
)

/** Whether the app is dark right now: chosen in Settings, or following the phone's dark mode. */
@Composable
fun Appearance.isDark(): Boolean = when (theme) {
    AppTheme.SYSTEM -> isSystemInDarkTheme()
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
}

/** The appearance for an activity's content. It's null until the settings have been read. */
@HiltViewModel
class AppearanceViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {

    val appearance: StateFlow<Appearance?> = settings.settings
        .map { Appearance(it.appTheme, it.dynamicColor) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/**
 * [PustakaTheme] with the app's chosen appearance. The status and navigation bar icons follow it
 * too, so they stay readable when the app is dark and the phone isn't, or the other way round.
 */
@Composable
fun ComponentActivity.PustakaAppTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val dark = appearance.isDark()
    DisposableEffect(dark) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
        )
        onDispose { }
    }
    PustakaTheme(darkTheme = dark, dynamicColor = appearance.dynamicColor, content = content)
}

/** The scrims `enableEdgeToEdge` uses by default behind three-button navigation. */
private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
