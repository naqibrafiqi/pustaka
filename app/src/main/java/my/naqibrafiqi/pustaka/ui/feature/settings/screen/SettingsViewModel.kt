package my.naqibrafiqi.pustaka.ui.feature.settings.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.data.library.access.PickedFolder
import my.naqibrafiqi.pustaka.data.library.scan.LibraryScans
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.data.settings.AppTheme
import my.naqibrafiqi.pustaka.data.settings.DefaultLibraryFolder
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import my.naqibrafiqi.pustaka.data.storage.StorageReport
import my.naqibrafiqi.pustaka.data.storage.StorageUsage
import javax.inject.Inject

data class SettingsUiState(
    val libraryFolderPath: String,
    val defaultLibraryFolderPath: String,
    val isDefaultLibraryFolder: Boolean,
    val hiddenSeries: Int,
    val hiddenBooks: Int,
    val appTheme: AppTheme,
    val dynamicColor: Boolean,
) {
    /** "2 series and 1 book", or "Nothing hidden". */
    val hiddenLabel: String
        get() {
            val parts = listOfNotNull(
                hiddenSeries.takeIf { it > 0 }?.let { "$it series" },
                hiddenBooks.takeIf { it > 0 }?.let { if (it == 1) "1 book" else "$it books" },
            )
            return if (parts.isEmpty()) "Nothing hidden" else parts.joinToString(" and ")
        }
}

/** Switching the library to another folder, waiting for the user to confirm. */
data class FolderChange(val path: String, val isDefault: Boolean)

sealed interface SettingsEvent {
    data class Message(val text: String) : SettingsEvent

    /** The library should be scanned, with the result shown to the user. */
    data object Rescan : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    seriesRepository: SeriesRepository,
    books: BookRepository,
    private val storageUsage: StorageUsage,
    scans: LibraryScans,
    @param:DefaultLibraryFolder private val defaultLibraryFolderPath: String,
) : ViewModel() {

    val state: StateFlow<SettingsUiState?> = combine(
        settings.settings,
        seriesRepository.observeHiddenSeries().map { it.size },
        books.observeHiddenBooks().map { it.size },
    ) { appSettings, hiddenSeries, hiddenBooks ->
        SettingsUiState(
            libraryFolderPath = appSettings.libraryFolderPath,
            defaultLibraryFolderPath = defaultLibraryFolderPath,
            isDefaultLibraryFolder = appSettings.isDefaultLibraryFolder,
            hiddenSeries = hiddenSeries,
            hiddenBooks = hiddenBooks,
            appTheme = appSettings.appTheme,
            dynamicColor = appSettings.dynamicColor,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _storage = MutableStateFlow<StorageReport?>(null)

    /** Measured when Settings opens and again after each scan. */
    val storage: StateFlow<StorageReport?> = _storage.asStateFlow()

    private val _folderChange = MutableStateFlow<FolderChange?>(null)

    /** The confirmation shown before switching folders. */
    val folderChange: StateFlow<FolderChange?> = _folderChange.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    init {
        measureStorage()
        viewModelScope.launch {
            // Scans add books and bring back covers, so the numbers are measured again after each.
            scans.status.map { it.isScanning }.distinctUntilChanged().drop(1).filter { !it }.collect { measureStorage() }
        }
    }

    /** A folder chosen in the picker. Nothing changes until it's confirmed. */
    fun onFolderPicked(picked: PickedFolder) {
        viewModelScope.launch {
            when (picked) {
                PickedFolder.NotInternalStorage -> send(
                    "Choose a folder on the phone's internal storage. SD cards and cloud folders can't be used."
                )
                is PickedFolder.Path -> if (picked.path == settings.settings.first().libraryFolderPath) {
                    send("That's already your library folder.")
                } else {
                    _folderChange.value = FolderChange(picked.path, isDefault = picked.path == defaultLibraryFolderPath)
                }
            }
        }
    }

    fun onFolderPickerUnavailable() {
        viewModelScope.launch { send("This phone has no folder picker.") }
    }

    fun useDefaultFolder() {
        _folderChange.value = FolderChange(defaultLibraryFolderPath, isDefault = true)
    }

    fun confirmFolderChange() {
        val change = _folderChange.value ?: return
        _folderChange.value = null
        viewModelScope.launch {
            if (change.isDefault) settings.resetLibraryFolderPath() else settings.setLibraryFolderPath(change.path)
            // The scan creates the folder if it isn't there, and marks books that aren't in it as
            // missing (decision D16).
            _events.send(SettingsEvent.Rescan)
        }
    }

    fun dismissFolderChange() {
        _folderChange.value = null
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch { settings.setAppTheme(theme) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }

    /** Deletes the cached covers, then scans so they're read from the books again. */
    fun clearCovers() {
        viewModelScope.launch {
            storageUsage.clearCovers()
            measureStorage()
            _events.send(SettingsEvent.Rescan)
        }
    }

    private fun measureStorage() {
        viewModelScope.launch { _storage.value = runCatching { storageUsage.measure() }.getOrNull() }
    }

    private suspend fun send(message: String) = _events.send(SettingsEvent.Message(message))

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
