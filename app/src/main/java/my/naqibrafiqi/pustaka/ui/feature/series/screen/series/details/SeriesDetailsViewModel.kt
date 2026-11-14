package my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.ui.model.BookItem
import my.naqibrafiqi.pustaka.ui.model.SeriesProgress
import my.naqibrafiqi.pustaka.ui.model.fileSizeLabel
import my.naqibrafiqi.pustaka.ui.model.genresOf
import my.naqibrafiqi.pustaka.ui.model.toItem

sealed interface SeriesDetailsUiState {

    data object Loading : SeriesDetailsUiState

    data class Error(val message: String) : SeriesDetailsUiState

    /** The series was deleted, for example because its folder went and it had no books left. */
    data object NotFound : SeriesDetailsUiState

    data class Ready(
        val name: String,
        val author: String?,
        val illustrator: String?,
        val synopsis: String?,
        /** The main genre first, then the tags. */
        val genres: List<String>,
        /** Covers for the fan in the header: the first three volumes, or the series' own cover. */
        val headerCovers: List<String?>,
        val volumeCount: Int,
        val finishedCount: Int,
        /** Finished volumes out of all volumes, from 0 to 100. */
        val percent: Int,
        val folderPath: String,
        /** The size of the volumes that are on disk, for example "14.2 MB". */
        val totalSize: String,
        /** Volumes whose file was found on the last scan. See [volumeCount] for all of them. */
        val indexedCount: Int,
        val volumes: List<BookItem>,
        val isGrid: Boolean,
    ) : SeriesDetailsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = SeriesDetailsViewModel.Factory::class)
class SeriesDetailsViewModel @AssistedInject constructor(
    @Assisted private val seriesId: Long,
    private val seriesRepository: SeriesRepository,
    private val books: BookRepository,
    private val reading: ReadingRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(seriesId: Long): SeriesDetailsViewModel
    }

    private val isGrid = savedState.getMutableStateFlow(KEY_GRID, true)
    private val attempt = MutableStateFlow(0)

    val state: StateFlow<SeriesDetailsUiState> = attempt
        .flatMapLatest {
            combine(
                seriesRepository.observeSeries(seriesId),
                books.observeVolumes(seriesId),
                reading.observeAllProgress(),
                isGrid,
            ) { series, volumes, progress, grid ->
                if (series == null) SeriesDetailsUiState.NotFound else buildState(series, volumes, progress, grid)
            }.catch { emit(SeriesDetailsUiState.Error("Couldn't load this series.")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SeriesDetailsUiState.Loading)

    fun toggleLayout() {
        isGrid.value = !isGrid.value
    }

    fun retry() {
        attempt.value++
    }

    private fun buildState(
        series: Series,
        volumes: List<Book>,
        progress: Map<Long, ReadingProgress>,
        grid: Boolean,
    ): SeriesDetailsUiState.Ready {
        val seriesProgress = SeriesProgress(volumes)
        val onDisk = volumes.filterNot { it.isMissing }
        val volumeCovers = volumes.mapNotNull { it.details.coverPath }.take(HEADER_COVERS)
        return SeriesDetailsUiState.Ready(
            name = series.name.value,
            author = series.details.author,
            illustrator = series.details.illustrator,
            synopsis = series.details.synopsis,
            genres = genresOf(series),
            headerCovers = volumeCovers.ifEmpty { listOf(series.details.coverPath) },
            volumeCount = seriesProgress.total,
            finishedCount = seriesProgress.finished,
            percent = seriesProgress.percent,
            folderPath = series.folderPath,
            totalSize = fileSizeLabel(onDisk.sumOf { it.fileSize }),
            indexedCount = onDisk.size,
            volumes = volumes.map { it.toItem(progress[it.id], seriesProgress.labelOf(it)) },
            isGrid = grid,
        )
    }

    private companion object {
        const val KEY_GRID = "grid"
        const val HEADER_COVERS = 3
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
