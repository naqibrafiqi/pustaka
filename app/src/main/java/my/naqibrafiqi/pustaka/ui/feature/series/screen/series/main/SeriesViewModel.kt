package my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.data.library.CreateSeriesResult
import my.naqibrafiqi.pustaka.data.library.LibraryFiles
import my.naqibrafiqi.pustaka.data.library.VolumeOrder
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import my.naqibrafiqi.pustaka.ui.model.SeriesItem
import my.naqibrafiqi.pustaka.ui.model.toItem
import javax.inject.Inject

/** The "New series" dialog. */
data class CreateSeriesDialog(
    val name: String = "",
    val error: String? = null,
    val isCreating: Boolean = false,
)

sealed interface SeriesUiState {

    data object Loading : SeriesUiState

    data class Error(val message: String) : SeriesUiState

    data class Ready(
        /** Sorted by name. Standalone books aren't series, so they're only in Library. */
        val series: List<SeriesItem>,
        val libraryFolderPath: String,
    ) : SeriesUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val seriesRepository: SeriesRepository,
    private val books: BookRepository,
    private val settings: SettingsRepository,
    private val libraryFiles: LibraryFiles,
) : ViewModel() {

    private val attempt = MutableStateFlow(0)

    val state: StateFlow<SeriesUiState> = attempt
        .flatMapLatest {
            combine(
                seriesRepository.observeSeries(),
                books.observeLibraryBooks(),
                settings.settings.map { it.libraryFolderPath },
            ) { allSeries, library, folderPath ->
                val volumesBySeries = library.filterNot { it.isStandalone }.groupBy { it.seriesId }
                SeriesUiState.Ready(
                    series = allSeries.map { series ->
                        series.toItem(VolumeOrder.sort(volumesBySeries[series.id].orEmpty()))
                    },
                    libraryFolderPath = folderPath,
                ) as SeriesUiState
            }.catch { emit(SeriesUiState.Error("Couldn't load your series.")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SeriesUiState.Loading)

    private val _createDialog = MutableStateFlow<CreateSeriesDialog?>(null)

    /** The "New series" dialog, while it's open. */
    val createDialog: StateFlow<CreateSeriesDialog?> = _createDialog.asStateFlow()

    private val _created = Channel<Long>(Channel.BUFFERED)

    /** The id of each series created here, so its details can be opened. */
    val created: Flow<Long> = _created.receiveAsFlow()

    fun openCreateDialog() {
        _createDialog.value = CreateSeriesDialog()
    }

    fun changeNewName(name: String) {
        _createDialog.update { it?.copy(name = name, error = null) }
    }

    fun dismissCreateDialog() {
        _createDialog.value = null
    }

    /** Makes a new empty folder in the library folder, which becomes the series. */
    fun createSeries() {
        val dialog = _createDialog.value ?: return
        if (dialog.isCreating) return
        _createDialog.value = dialog.copy(isCreating = true, error = null)
        viewModelScope.launch {
            when (val result = libraryFiles.createSeries(dialog.name)) {
                is CreateSeriesResult.Created -> {
                    _createDialog.value = null
                    _created.send(result.seriesId)
                }
                is CreateSeriesResult.Invalid -> _createDialog.update { it?.copy(isCreating = false, error = result.message) }
                is CreateSeriesResult.Failed -> _createDialog.update { it?.copy(isCreating = false, error = result.message) }
            }
        }
    }

    fun retry() {
        attempt.value++
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
