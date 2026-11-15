package my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.library.DeleteResult
import my.naqibrafiqi.pustaka.data.library.FolderContents
import my.naqibrafiqi.pustaka.data.library.LibraryFiles
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.ui.model.BookItem
import my.naqibrafiqi.pustaka.ui.model.DetailsEvent
import my.naqibrafiqi.pustaka.ui.model.SeriesProgress
import my.naqibrafiqi.pustaka.ui.model.fileSizeLabel
import my.naqibrafiqi.pustaka.ui.model.genresOf
import my.naqibrafiqi.pustaka.ui.model.toItem

/** The steps of "Delete series". See "Deleting" in `docs/local_storage_and_library.md`. */
sealed interface SeriesDeleteDialog {
    val name: String
    val folderPath: String

    /** "Remove from library" or "Delete folder and files". */
    data class Choose(
        override val name: String,
        override val folderPath: String,
        val contents: FolderContents,
    ) : SeriesDeleteDialog

    /** The last check before the folder and its files are deleted. */
    data class ConfirmDeleteFiles(
        override val name: String,
        override val folderPath: String,
        val contents: FolderContents,
    ) : SeriesDeleteDialog

    /** The folder has nothing in it, so it's just deleted. */
    data class ConfirmDeleteEmptyFolder(
        override val name: String,
        override val folderPath: String,
    ) : SeriesDeleteDialog
}

sealed interface SeriesDetailsUiState {

    data object Loading : SeriesDetailsUiState

    data class Error(@param:StringRes val message: Int) : SeriesDetailsUiState

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
    private val libraryFiles: LibraryFiles,
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
            }.catch { emit(SeriesDetailsUiState.Error(R.string.series_details_error)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SeriesDetailsUiState.Loading)

    private val _deleteDialog = MutableStateFlow<SeriesDeleteDialog?>(null)

    /** The delete dialog that's showing, if any. */
    val deleteDialog: StateFlow<SeriesDeleteDialog?> = _deleteDialog.asStateFlow()

    private val _events = Channel<DetailsEvent>(Channel.BUFFERED)
    val events: Flow<DetailsEvent> = _events.receiveAsFlow()

    /**
     * "Delete series". A folder with nothing in it is simply deleted after a confirmation. Otherwise
     * the user picks between removing the series from the library and deleting its files.
     */
    fun requestDelete() {
        viewModelScope.launch {
            val series = seriesRepository.getSeries(seriesId) ?: return@launch
            val contents = libraryFiles.contentsOf(series.folderPath)
            _deleteDialog.value = if (contents.files == 0) {
                SeriesDeleteDialog.ConfirmDeleteEmptyFolder(series.name.value, series.folderPath)
            } else {
                SeriesDeleteDialog.Choose(series.name.value, series.folderPath, contents)
            }
        }
    }

    fun chooseDeleteFiles() {
        val choose = _deleteDialog.value as? SeriesDeleteDialog.Choose ?: return
        _deleteDialog.value = SeriesDeleteDialog.ConfirmDeleteFiles(choose.name, choose.folderPath, choose.contents)
    }

    /** Hides the series and its volumes. The folder isn't touched. */
    fun removeFromLibrary() {
        _deleteDialog.value = null
        viewModelScope.launch {
            seriesRepository.setHidden(seriesId, true)
            _events.send(DetailsEvent.Closed)
        }
    }

    /** Deletes the folder and everything in it, after the user has confirmed. */
    fun deleteFolder() {
        _deleteDialog.value = null
        viewModelScope.launch {
            when (val result = libraryFiles.deleteSeriesFolder(seriesId)) {
                DeleteResult.Deleted -> _events.send(DetailsEvent.Closed)
                is DeleteResult.Failed -> _events.send(DetailsEvent.Message(result.message))
            }
        }
    }

    fun dismissDeleteDialog() {
        _deleteDialog.value = null
    }

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
