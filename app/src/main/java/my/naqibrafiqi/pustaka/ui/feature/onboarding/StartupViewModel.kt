package my.naqibrafiqi.pustaka.ui.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.data.library.access.StorageAccess
import my.naqibrafiqi.pustaka.data.library.scan.LibraryScans
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import javax.inject.Inject

/** What the app shows at the top level, before the library. */
enum class StartupScreen {
    /** Settings haven't loaded yet. */
    LOADING,

    /** First launch: explains why the app needs access to files. */
    WELCOME,

    /** Access was refused or taken away. Shows a "Grant access" button. */
    ACCESS_NEEDED,

    /** The library. */
    READY;

    companion object {
        fun of(hasAccess: Boolean, hasSeenWelcome: Boolean): StartupScreen = when {
            hasAccess -> READY
            hasSeenWelcome -> ACCESS_NEEDED
            else -> WELCOME
        }
    }
}

data class StartupState(
    val screen: StartupScreen,
    val libraryFolderPath: String = "",
)

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val storageAccess: StorageAccess,
    private val settings: SettingsRepository,
    private val scans: LibraryScans,
) : ViewModel() {

    private val hasAccess = MutableStateFlow(storageAccess.hasAccess())

    val state: StateFlow<StartupState> = combine(hasAccess, settings.settings) { access, appSettings ->
        StartupState(
            screen = StartupScreen.of(access, appSettings.hasSeenWelcome),
            libraryFolderPath = appSettings.libraryFolderPath,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartupState(StartupScreen.LOADING))

    /**
     * Checks access again, for example when the user comes back from the system settings screen.
     * When access has just been granted, the first scan starts, which also creates the folder.
     */
    fun refreshAccess() {
        val granted = storageAccess.hasAccess()
        val wasGranted = hasAccess.value
        hasAccess.value = granted
        if (granted && !wasGranted) scans.requestScan()
    }

    fun onWelcomeSeen() {
        viewModelScope.launch { settings.setHasSeenWelcome() }
    }
}
