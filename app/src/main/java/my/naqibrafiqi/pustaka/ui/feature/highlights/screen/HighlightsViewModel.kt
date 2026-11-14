package my.naqibrafiqi.pustaka.ui.feature.highlights.screen

import android.net.Uri
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
import my.naqibrafiqi.pustaka.data.export.ExportFiles
import my.naqibrafiqi.pustaka.data.model.HighlightColor
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.HighlightRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

sealed interface HighlightsUiState {

    data object Loading : HighlightsUiState

    data class Error(val message: String) : HighlightsUiState

    /**
     * @property allGroups every highlight in the library, grouped by series and book.
     * @property groups the ones matching [query].
     */
    data class Ready(
        val allGroups: List<SeriesHighlights>,
        val groups: List<SeriesHighlights>,
        val query: String,
    ) : HighlightsUiState {
        val totalCount: Int get() = allGroups.sumOf { it.highlightCount }
        val isSearching: Boolean get() = query.isNotBlank()
    }
}

/** Something for the screen to do once. */
sealed interface HighlightsEvent {

    /** Opens the share sheet with an exported file (decision D11). */
    data class Share(val uri: Uri, val fileName: String) : HighlightsEvent

    data class Message(val text: String) : HighlightsEvent
}

/** The Highlights tab: every highlight, grouped by series and book, with search and export. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HighlightsViewModel @Inject constructor(
    private val highlights: HighlightRepository,
    books: BookRepository,
    series: SeriesRepository,
    private val exportFiles: ExportFiles,
    private val clock: Clock,
) : ViewModel() {

    private val attempt = MutableStateFlow(0)
    private val query = MutableStateFlow("")

    val state: StateFlow<HighlightsUiState> = attempt
        .flatMapLatest {
            combine(
                highlights.observeAllHighlights(),
                books.observeLibraryBooks(),
                series.observeSeries(),
                query,
            ) { all, library, allSeries, query ->
                val groups = groupHighlights(all, library, allSeries)
                HighlightsUiState.Ready(allGroups = groups, groups = groups.search(query), query = query) as HighlightsUiState
            }.catch { emit(HighlightsUiState.Error("Couldn't load your highlights.")) }
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

    /** Exports every highlight in the library, whatever the search (decision D12). */
    fun exportAll() {
        val groups = currentGroups()
        if (groups.isEmpty()) return
        export(groups, HighlightsMarkdown.fileName(null))
    }

    /** Exports one book's highlights (decision D12). */
    fun exportBook(bookId: Long) {
        val groups = currentGroups().mapNotNull { group ->
            group.books.filter { it.bookId == bookId }.takeIf { it.isNotEmpty() }?.let { group.copy(books = it) }
        }
        val title = groups.firstOrNull()?.books?.firstOrNull()?.title ?: return
        export(groups, HighlightsMarkdown.fileName(title))
    }

    fun retry() {
        attempt.value++
    }

    private fun export(groups: List<SeriesHighlights>, fileName: String) {
        viewModelScope.launch {
            val markdown = HighlightsMarkdown.build(groups, clock.instant().atZone(ZoneId.systemDefault()).toLocalDate())
            val uri = exportFiles.write(fileName, markdown)
            _events.send(
                if (uri != null) HighlightsEvent.Share(uri, fileName) else HighlightsEvent.Message("Couldn't export your highlights.")
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
