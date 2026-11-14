package my.naqibrafiqi.pustaka.ui.feature.library.screen

import androidx.lifecycle.SavedStateHandle
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
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import my.naqibrafiqi.pustaka.ui.model.BookItem
import my.naqibrafiqi.pustaka.ui.model.SeriesProgress
import my.naqibrafiqi.pustaka.ui.model.pageLabel
import my.naqibrafiqi.pustaka.ui.model.readAgoLabel
import my.naqibrafiqi.pustaka.ui.model.toItem
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

enum class LibraryFilter(val label: String) {
    ALL("All books"),
    READING("Currently reading"),
    UNREAD("Unread");

    fun matches(book: Book): Boolean = when (this) {
        ALL -> true
        READING -> book.readingStatus == ReadingStatus.READING
        UNREAD -> book.readingStatus == ReadingStatus.UNREAD
    }
}

/** The book on the "Currently Reading" card. */
data class CurrentlyReading(
    val book: BookItem,
    /** "Read 32m ago". */
    val readAgo: String?,
    /** "Page 214 of 312". */
    val pageLabel: String?,
)

sealed interface LibraryUiState {

    data object Loading : LibraryUiState

    data class Error(val message: String) : LibraryUiState

    data class Ready(
        val currentlyReading: CurrentlyReading?,
        val filter: LibraryFilter,
        /** How many books each filter shows, for the chip labels. */
        val counts: Map<LibraryFilter, Int>,
        /** The books for [filter], newest first. */
        val books: List<BookItem>,
        val isGrid: Boolean,
        val libraryFolderPath: String,
    ) : LibraryUiState {
        val isLibraryEmpty: Boolean get() = counts[LibraryFilter.ALL] == 0
    }
}

/** Standalone books and the volumes of every series, newest first, with the filters and counts. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val books: BookRepository,
    private val reading: ReadingRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val filter = savedState.getMutableStateFlow(KEY_FILTER, LibraryFilter.ALL)
    private val isGrid = savedState.getMutableStateFlow(KEY_GRID, true)
    private val attempt = MutableStateFlow(0)

    val state: StateFlow<LibraryUiState> = attempt
        .flatMapLatest {
            combine(
                books.observeLibraryBooks(),
                reading.observeAllProgress(),
                settings.settings.map { it.libraryFolderPath },
                filter,
                isGrid,
            ) { library, progress, folderPath, selected, grid ->
                buildState(library, progress, folderPath, selected, grid)
            }.catch<LibraryUiState> { emit(LibraryUiState.Error("Couldn't load your library.")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryUiState.Loading)

    fun selectFilter(selected: LibraryFilter) {
        filter.value = selected
    }

    fun toggleLayout() {
        isGrid.value = !isGrid.value
    }

    fun retry() {
        attempt.value++
    }

    private fun buildState(
        library: List<Book>,
        progress: Map<Long, ReadingProgress>,
        folderPath: String,
        selected: LibraryFilter,
        grid: Boolean,
    ): LibraryUiState.Ready {
        val newestFirst = library.sortedWith(compareByDescending<Book> { it.addedAt }.thenByDescending { it.id })
        val volumeLabels = volumeLabels(library)
        return LibraryUiState.Ready(
            currentlyReading = currentlyReading(library, progress, volumeLabels),
            filter = selected,
            counts = LibraryFilter.entries.associateWith { f -> library.count(f::matches) },
            books = newestFirst.filter(selected::matches).map { it.toItem(progress[it.id], volumeLabels[it.id]) },
            isGrid = grid,
            libraryFolderPath = folderPath,
        )
    }

    /** The book being read that was opened last. Missing books can't be resumed. */
    private fun currentlyReading(
        library: List<Book>,
        progress: Map<Long, ReadingProgress>,
        volumeLabels: Map<Long, String>,
    ): CurrentlyReading? {
        val book = library
            .filter { it.readingStatus == ReadingStatus.READING && !it.isMissing }
            .maxWithOrNull(compareBy<Book, Instant?>(nullsFirst()) { it.lastOpenedAt }.thenBy { it.id })
            ?: return null
        val position = progress[book.id]?.position
        return CurrentlyReading(
            book = book.toItem(progress[book.id], volumeLabels[book.id]),
            readAgo = readAgoLabel(book.lastOpenedAt, clock.instant()),
            pageLabel = pageLabel(position, book.totalPositions),
        )
    }

    /** "Volume 4" for each volume of a series, by book id. */
    private fun volumeLabels(library: List<Book>): Map<Long, String> =
        library.filter { !it.isStandalone }
            .groupBy { it.seriesId }
            .values
            .flatMap { volumes ->
                val ordered = VolumeOrder.sort(volumes)
                val series = SeriesProgress(ordered)
                ordered.map { it.id to series.labelOf(it) }
            }
            .toMap()

    private companion object {
        const val KEY_FILTER = "filter"
        const val KEY_GRID = "grid"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
