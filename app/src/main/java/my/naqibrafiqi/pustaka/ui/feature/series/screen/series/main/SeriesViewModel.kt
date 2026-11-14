package my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import my.naqibrafiqi.pustaka.data.library.VolumeOrder
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import my.naqibrafiqi.pustaka.ui.model.SeriesItem
import my.naqibrafiqi.pustaka.ui.model.toItem
import javax.inject.Inject

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

    fun retry() {
        attempt.value++
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
