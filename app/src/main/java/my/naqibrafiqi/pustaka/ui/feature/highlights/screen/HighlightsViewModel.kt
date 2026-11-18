package my.naqibrafiqi.pustaka.ui.feature.highlights.screen

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
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
import kotlinx.coroutines.withContext
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.export.ExportFiles
import my.naqibrafiqi.pustaka.data.model.HighlightColor
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.BookmarkRepository
import my.naqibrafiqi.pustaka.data.repository.HighlightRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

sealed interface HighlightsUiState {

    data object Loading : HighlightsUiState

    data class Error(@param:StringRes val message: Int) : HighlightsUiState

    /**
     * @property allGroups every highlight in the library, grouped by series and book.
     * @property groups the ones matching [query].
     * @property allBookmarks every bookmark in the library, grouped the same way (#51, D33).
     * @property bookmarks the ones matching [query].
     */
    data class Ready(
        val allGroups: List<SeriesHighlights>,
        val groups: List<SeriesHighlights>,
        val query: String,
        val allBookmarks: List<SeriesBookmarks> = emptyList(),
        val bookmarks: List<SeriesBookmarks> = emptyList(),
    ) : HighlightsUiState {
        val totalCount: Int get() = allGroups.sumOf { it.highlightCount }
        val bookmarkCount: Int get() = allBookmarks.sumOf { it.bookmarkCount }
        val isSearching: Boolean get() = query.isNotBlank()
    }
}

/** Something for the screen to do once. */
sealed interface HighlightsEvent {

    /** Opens the share sheet with an exported file (decision D11). */
    data class Share(val uri: Uri, val fileName: String) : HighlightsEvent

    data class Message(@param:StringRes val text: Int) : HighlightsEvent
}

/**
 * The Highlights tab: every highlight and bookmark, grouped by series and book, with search, and
 * export for highlights.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HighlightsViewModel @Inject constructor(
    private val highlights: HighlightRepository,
    private val bookmarks: BookmarkRepository,
    books: BookRepository,
    series: SeriesRepository,
    private val exportFiles: ExportFiles,
    private val markdown: HighlightsMarkdown,
    private val clock: Clock,
) : ViewModel() {

    private val attempt = MutableStateFlow(0)
    private val query = MutableStateFlow("")

    val state: StateFlow<HighlightsUiState> = attempt
        .flatMapLatest {
            combine(
                highlights.observeAllHighlights(),
                bookmarks.observeAllBookmarks(),
                books.observeLibraryBooks(),
                series.observeSeries(),
                query,
            ) { all, allBookmarks, library, allSeries, query ->
                val groups = groupHighlights(all, library, allSeries)
                val bookmarkGroups = groupBookmarks(allBookmarks, library, allSeries)
                HighlightsUiState.Ready(
                    allGroups = groups,
                    groups = groups.search(query),
                    query = query,
                    allBookmarks = bookmarkGroups,
                    bookmarks = bookmarkGroups.searchBookmarks(query),
                ) as HighlightsUiState
            }.catch { emit(HighlightsUiState.Error(R.string.highlights_error)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HighlightsUiState.Loading)

    private val _openHighlightId = MutableStateFlow<Long?>(null)

    /** The highlight being changed, in the same sheet as the reader's. */
    val openHighlightId: StateFlow<Long?> = _openHighlightId.asStateFlow()

    private val _events = Channel<HighlightsEvent>(Channel.BUFFERED)
    val events: Flow<HighlightsEvent> = _events.receiveAsFlow()

    fun search(text: String) {
        query.value = text
    }

    fun openHighlight(id: Long) {
        _openHighlightId.value = id
    }

    fun closeHighlight() {
        _openHighlightId.value = null
    }

    /** Changes the highlight's colour or note. A blank note removes it. */
    fun updateHighlight(id: Long, color: HighlightColor, note: String?) {
        val current = findHighlight(id) ?: return
        val cleanNote = note?.trim()?.ifEmpty { null }
        if (current.color == color && current.note == cleanNote) return
        viewModelScope.launch {
            withContext(NonCancellable) { highlights.updateHighlight(id, cleanNote, color) }
        }
    }

    fun deleteHighlight(id: Long) {
        if (_openHighlightId.value == id) _openHighlightId.value = null
        viewModelScope.launch {
            withContext(NonCancellable) { highlights.deleteHighlight(id) }
        }
    }

    /** Removes a bookmark from the Bookmarks list, the same as in the reader's Contents sheet. */
    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            withContext(NonCancellable) { bookmarks.deleteBookmarks(listOf(id)) }
        }
    }

    /** Exports every highlight in the library, whatever the search (decision D12). */
    fun exportAll() {
        val groups = currentGroups()
        if (groups.isEmpty()) return
        export(groups, markdown.fileName(null))
    }

    /** Exports one book's highlights (decision D12). */
    fun exportBook(bookId: Long) {
        val groups = currentGroups().mapNotNull { group ->
            group.books.filter { it.bookId == bookId }.takeIf { it.isNotEmpty() }?.let { group.copy(books = it) }
        }
        val title = groups.firstOrNull()?.books?.firstOrNull()?.title ?: return
        export(groups, markdown.fileName(title))
    }

    fun retry() {
        attempt.value++
    }

    private fun export(groups: List<SeriesHighlights>, fileName: String) {
        viewModelScope.launch {
            val text = markdown.build(groups, clock.instant().atZone(ZoneId.systemDefault()).toLocalDate())
            val uri = exportFiles.write(fileName, text)
            _events.send(
                if (uri != null) HighlightsEvent.Share(uri, fileName) else HighlightsEvent.Message(R.string.highlights_export_failed)
            )
        }
    }

    /** Every highlight grouped, ignoring the search. */
    private fun currentGroups(): List<SeriesHighlights> = (state.value as? HighlightsUiState.Ready)?.allGroups.orEmpty()

    private fun findHighlight(id: Long) =
        currentGroups().asSequence().flatMap { it.books }.flatMap { it.highlights }.firstOrNull { it.id == id }?.highlight

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
