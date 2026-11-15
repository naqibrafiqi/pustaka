package my.naqibrafiqi.pustaka.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.data.library.scan.CompletedScan
import my.naqibrafiqi.pustaka.data.library.scan.LibraryScans
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import my.naqibrafiqi.pustaka.util.text.UiText
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Pull-to-refresh, "Rescan library" in Settings, and the message shown when a scan ends. One
 * instance is shared by the whole app.
 */
@HiltViewModel
class LibraryScanViewModel @Inject constructor(
    private val scans: LibraryScans,
    private val clock: Clock,
    settings: SettingsRepository,
) : ViewModel() {

    private val reports = ScanReports(since = clock.instant())

    private val userRequestedAt = MutableStateFlow<Instant?>(null)

    /** True from a pull-to-refresh or "Rescan library" until that scan ends. */
    val isRefreshing: StateFlow<Boolean> = userRequestedAt
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Any scan, including the automatic ones. */
    val isScanning: StateFlow<Boolean> = scans.status
        .map { it.isScanning }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val libraryFolderPath: StateFlow<String?> = settings.settings
        .map { it.libraryFolderPath }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            scans.status.collect { status ->
                val scan = reports.newlyFinished(status.lastScan) ?: return@collect
                val requestedAt = userRequestedAt.value
                val forUser = requestedAt != null && !scan.finishedAt.isBefore(requestedAt)
                if (forUser) userRequestedAt.value = null
                ScanMessages.forOutcome(scan.outcome, requestedByUser = forUser)?.let { _messages.send(it) }
            }
        }
    }

    fun refresh() {
        if (userRequestedAt.value == null) userRequestedAt.value = clock.instant()
        scans.requestScan()
    }
}

/**
 * Picks out the scans to report on. WorkManager keeps the last scan's outcome after the app is
 * closed, so scans that ended before [since] (when the app started) are skipped, as are scans that
 * were already reported.
 */
internal class ScanReports(private val since: Instant) {

    private val reported = HashSet<UUID>()

    fun newlyFinished(scan: CompletedScan?): CompletedScan? =
        scan?.takeIf { !it.finishedAt.isBefore(since) && reported.add(it.id) }
}
