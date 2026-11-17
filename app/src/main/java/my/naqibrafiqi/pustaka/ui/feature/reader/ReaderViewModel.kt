package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.library.epub.OpenFailure
import my.naqibrafiqi.pustaka.data.library.epub.OpenResult
import my.naqibrafiqi.pustaka.data.library.epub.ReadiumToolkit
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.Highlight
import my.naqibrafiqi.pustaka.data.model.HighlightColor
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.HighlightRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.data.settings.ReaderSettings
import my.naqibrafiqi.pustaka.data.settings.ReaderSettingsRepository
import my.naqibrafiqi.pustaka.ui.model.SeriesProgress
import my.naqibrafiqi.pustaka.util.text.UiText
import my.naqibrafiqi.pustaka.util.text.uiText
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import java.io.File
import java.time.Clock
import javax.inject.Inject
import kotlin.math.floor

sealed interface ReaderUiState {

    data object Loading : ReaderUiState

    data class Failed(@param:StringRes val message: Int) : ReaderUiState

    /**
     * The book is open.
     *
     * @property initialLocator where to start, or null for the beginning.
     * @property positions one locator per page, used by the progress bar to jump to a page.
     */
    class Ready(
        val title: String,
        val publication: Publication,
        val initialLocator: Locator?,
        val positions: List<Locator>,
        val tableOfContents: List<TocEntry>,
    ) : ReaderUiState {
        val pageCount: Int get() = positions.size
        val readingOrder: List<String> get() = publication.readingOrder.map { it.href.toString() }
    }
}

/** Where the reader is, for the progress bar and the table of contents. */
data class ReaderPosition(
    /** Starting at 1, or null before the first page has been shown. */
    val page: Int? = null,
    val pageCount: Int = 0,
    /** From 0 to 100, rounded down. Readium measures where the page starts, so the last page is just under 100. */
    val percent: Int = 0,
    val chapterTitle: String? = null,
    /** The index in [ReaderUiState.Ready.tableOfContents] of the chapter being read. */
    val tocIndex: Int? = null,
) {
    /** "Page 12 of 240". */
    val pageLabel: UiText? get() = page?.let { uiText(R.string.page_of_total, it, pageCount) }
}

/**
 * The card shown on reaching the last page (decision D10). The book is already marked finished.
 *
 * @property next the volume to read next, or null for the last volume or a standalone book.
 */
data class FinishCard(
    val title: String,
    val seriesName: String?,
    val next: NextVolume?,
)

/**
 * @property label "Volume 2".
 * @property fromStart a finished volume is read again from its first page.
 */
data class NextVolume(val bookId: Long, val title: String, val label: UiText, val fromStart: Boolean)

/** A highlight in the book being read, with its place in the book ready for the reader to draw. */
data class ReaderHighlight(val highlight: Highlight, val locator: Locator) {
    val id: Long get() = highlight.id
}

/**
 * Opens a book in the reader and keeps its reading position saved. The position is written shortly
 * after each page turn, and straight away when the reader goes into the background.
 *
 * It also records reading sessions (see [SessionTracker]), applies the reader settings, marks
 * the book finished on its last page, and keeps the book's highlights.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val books: BookRepository,
    private val reading: ReadingRepository,
    private val toolkit: ReadiumToolkit,
    private val seriesRepository: SeriesRepository,
    private val readerSettings: ReaderSettingsRepository,
    private val highlightRepository: HighlightRepository,
    private val clock: Clock,
) : ViewModel() {

    private val bookId: Long = checkNotNull(savedStateHandle[ReaderActivity.EXTRA_BOOK_ID]) {
        "The reader needs a book id."
    }
    private val fromStart: Boolean = savedStateHandle[ReaderActivity.EXTRA_FROM_START] ?: false

    /** Where to open the book instead of the saved position, for example at a highlight. */
    private val openAt: Locator? = savedStateHandle.get<String>(ReaderActivity.EXTRA_LOCATOR)?.let(::parseLocator)

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val _position = MutableStateFlow(ReaderPosition())
    val position: StateFlow<ReaderPosition> = _position.asStateFlow()

    private val _settings = MutableStateFlow(ReaderSettings())

    /** The reader settings, loaded before the book is shown so it's laid out once. */
    val settings: StateFlow<ReaderSettings> = _settings.asStateFlow()

    private val _finishCard = MutableStateFlow<FinishCard?>(null)
    val finishCard: StateFlow<FinishCard?> = _finishCard.asStateFlow()

    /** The book's highlights, drawn on the page. Any whose place can't be read are left out. */
    val highlights: StateFlow<List<ReaderHighlight>> = highlightRepository.observeHighlights(bookId)
        .map { list -> list.mapNotNull { highlight -> parseLocator(highlight.locator)?.let { ReaderHighlight(highlight, it) } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _openHighlightId = MutableStateFlow<Long?>(null)

    /** The highlight being looked at or changed, after tapping it on the page. */
    val openHighlightId: StateFlow<Long?> = _openHighlightId.asStateFlow()

    private val returnTracker = ReturnTracker<Locator>()
    private val _returnPoint = MutableStateFlow<ReturnPoint<Locator>?>(null)

    /** The page before the last jump, offered as "Back to page 42" until reading goes on (#35, D26). */
    val returnPoint: StateFlow<ReturnPoint<Locator>?> = _returnPoint.asStateFlow()

    private var publication: Publication? = null
    private var book: Book? = null
    private val sessions = SessionTracker()

    /** The last page has been reached during this reading, so the card isn't shown twice. */
    private var finishHandled = false

    /** The newest locator from the navigator, saved after [SAVE_DELAY_MS] without a new one. */
    private val latestLocator = MutableStateFlow<Locator?>(null)
    private var lastSaved: Locator? = null

    init {
        viewModelScope.launch { open() }
        viewModelScope.launch {
            latestLocator.filterNotNull().debounce(SAVE_DELAY_MS).collect(::save)
        }
    }

    /** Called by the navigator every time the visible page changes. */
    fun onLocatorChanged(locator: Locator) {
        val ready = _state.value as? ReaderUiState.Ready ?: return
        latestLocator.value = locator
        val position = positionOf(locator, ready)
        _position.value = position

        position.page?.let { page ->
            sessions.onPage(page, clock.instant())?.let(::record)
            if (returnTracker.onPage(page)) _returnPoint.value = null
        }
        if (!finishHandled && isLastPage(locator, ready)) {
            finishHandled = true
            viewModelScope.launch { finish() }
        }
    }

    /**
     * Called just before jumping to [target] from the table of contents or the progress bar, so the
     * page being left can be offered to go back to.
     */
    fun onJump(target: Locator) {
        val ready = _state.value as? ReaderUiState.Ready ?: return
        val from = latestLocator.value ?: return
        returnTracker.onJump(from, fromPage = _position.value.page, toPage = pageOf(target, ready))
        _returnPoint.value = returnTracker.point
    }

    /** Where to go for "Back to page 42", which is then no longer offered. */
    fun takeReturnPoint(): Locator? = returnTracker.take().also { _returnPoint.value = null }

    /** The reader is showing again, after being in the background. */
    fun onReaderResumed() {
        if (sessions.isActive) return
        _position.value.page?.let { sessions.onPage(it, clock.instant()) }
    }

    /** The reader went into the background or is closing: saves the position and the session. */
    fun onReaderPaused() {
        saveNow()
        sessions.stop(clock.instant())?.let(::record)
        if (_state.value is ReaderUiState.Ready) {
            // "Read 32m ago" counts from when the reading stopped.
            viewModelScope.launch { withContext(NonCancellable) { books.markOpened(bookId) } }
        }
    }

    fun updateSettings(settings: ReaderSettings) {
        _settings.value = settings
        viewModelScope.launch { readerSettings.update(settings) }
    }

    fun resetSettings() {
        _settings.value = ReaderSettings()
        viewModelScope.launch { readerSettings.reset() }
    }

    fun dismissFinishCard() {
        _finishCard.value = null
    }

    /**
     * Highlights the selected text straight away, in the colour used last (decision D14).
     *
     * @param selection where the selection is, with the selected text and the text around it.
     */
    fun addHighlight(selection: Locator) {
        val text = selection.text.highlight?.trim()?.ifEmpty { null } ?: return
        viewModelScope.launch {
            withContext(NonCancellable) {
                highlightRepository.addHighlight(
                    bookId = bookId,
                    locator = selection.toJSON().toString(),
                    text = text,
                    note = null,
                    color = highlightRepository.lastUsedColor(),
                )
            }
        }
    }

    fun openHighlight(id: Long) {
        _openHighlightId.value = id
    }

    fun closeHighlight() {
        _openHighlightId.value = null
    }

    /** Changes the highlight's colour or note. A blank note removes it. */
    fun updateHighlight(id: Long, color: HighlightColor, note: String?) {
        val current = highlights.value.firstOrNull { it.id == id }?.highlight ?: return
        val cleanNote = note?.trim()?.ifEmpty { null }
        if (current.color == color && current.note == cleanNote) return
        viewModelScope.launch {
            withContext(NonCancellable) { highlightRepository.updateHighlight(id, cleanNote, color) }
        }
    }

    fun deleteHighlight(id: Long) {
        if (_openHighlightId.value == id) _openHighlightId.value = null
        viewModelScope.launch {
            withContext(NonCancellable) { highlightRepository.deleteHighlight(id) }
        }
    }

    /** Saves the position now, for when the reader goes into the background or closes. */
    fun saveNow() {
        val locator = latestLocator.value ?: return
        viewModelScope.launch { save(locator) }
    }

    private suspend fun open() {
        val book = books.getBook(bookId)
        if (book == null) return fail(R.string.reader_error_not_in_library)
        if (book.isMissing) return fail(R.string.reader_error_missing)

        val opened = when (val result = toolkit.open(File(book.filePath), forReading = true)) {
            is OpenResult.Failed -> return fail(messageFor(result.reason))
            is OpenResult.Opened -> result.publication
        }
        publication = opened
        this.book = book
        _settings.value = readerSettings.settings.first()

        if (fromStart) reading.clearProgress(bookId)
        val saved = if (fromStart) null else reading.observeProgress(bookId).first()
        val initialLocator = openAt ?: saved?.locator?.let(::parseLocator)

        // Reading a finished book again from the start makes it a book being read.
        if (book.readingStatus == ReadingStatus.UNREAD || (fromStart && book.readingStatus == ReadingStatus.FINISHED)) {
            books.setReadingStatus(bookId, ReadingStatus.READING)
        }
        books.markOpened(bookId)

        val ready = ReaderUiState.Ready(
            title = book.name.value,
            publication = opened,
            initialLocator = initialLocator,
            positions = runCatching { opened.positions() }.getOrDefault(emptyList()),
            tableOfContents = flattenTableOfContents(opened.tableOfContents),
        )
        _position.value = initialLocator?.let { positionOf(it, ready) } ?: ReaderPosition(pageCount = ready.pageCount)
        // Opening at a highlight is a jump too, so the saved page is offered to go back to.
        val savedLocator = saved?.locator?.let(::parseLocator)
        if (openAt != null && savedLocator != null) {
            returnTracker.onJump(savedLocator, fromPage = pageOf(savedLocator, ready), toPage = pageOf(openAt, ready))
            _returnPoint.value = returnTracker.point
        }
        _state.value = ready
    }

    private suspend fun save(locator: Locator) {
        if (locator == lastSaved) return
        // Runs to the end even if the reader closes while it's being written.
        withContext(NonCancellable) {
            reading.saveProgress(
                bookId = bookId,
                locator = locator.toJSON().toString(),
                progression = locator.locations.totalProgression ?: 0.0,
                position = locator.locations.position,
            )
        }
        lastSaved = locator
    }

    private fun record(session: TrackedSession) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                reading.recordSession(bookId, session.startedAt, session.endedAt, session.pagesRead)
            }
        }
    }

    /** Marks the book finished and offers the next volume (decision D10). */
    private suspend fun finish() {
        val current = book ?: return
        books.setReadingStatus(bookId, ReadingStatus.FINISHED)
        val seriesId = current.seriesId
        val volumes = seriesId?.let { books.observeVolumes(it).first() }.orEmpty()
        val next = nextVolume(volumes, current)
        _finishCard.value = FinishCard(
            title = current.name.value,
            seriesName = seriesId?.let { seriesRepository.observeSeries(it).first()?.name?.value },
            next = next?.let {
                NextVolume(
                    bookId = it.id,
                    title = it.name.value,
                    label = SeriesProgress(volumes).labelOf(it),
                    fromStart = it.readingStatus == ReadingStatus.FINISHED,
                )
            },
        )
    }

    private fun fail(@StringRes message: Int) {
        _state.value = ReaderUiState.Failed(message)
    }

    override fun onCleared() {
        publication?.close()
        publication = null
    }

    companion object {
        const val SAVE_DELAY_MS = 500L

        @StringRes
        fun messageFor(reason: OpenFailure): Int = when (reason) {
            OpenFailure.NOT_READABLE -> R.string.reader_error_not_readable
            OpenFailure.NOT_EPUB -> R.string.reader_error_not_epub
            OpenFailure.PROTECTED -> R.string.reader_error_protected
        }

        /** A saved locator that can't be read any more just means starting from the beginning. */
        fun parseLocator(json: String): Locator? =
            runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()

        /** The last page, or the very end of the book when it's scrolled. */
        fun isLastPage(locator: Locator, ready: ReaderUiState.Ready): Boolean {
            val page = locator.locations.position
            if (page != null && ready.pageCount > 0) return page >= ready.pageCount
            return (locator.locations.totalProgression ?: 0.0) >= END_OF_BOOK
        }

        private const val END_OF_BOOK = 0.999

        /**
         * The page [locator] is on. Places such as a chapter's start or a highlight don't always say,
         * so it's then the last page that starts at or before it in the same chapter.
         */
        fun pageOf(locator: Locator, ready: ReaderUiState.Ready): Int? {
            locator.locations.position?.let { return it }
            val href = locator.href.toString().substringBefore('#')
            val progression = locator.locations.progression ?: 0.0
            val chapter = ready.positions.filter { it.href.toString().substringBefore('#') == href }
            val page = chapter.lastOrNull { (it.locations.progression ?: 0.0) <= progression + 1e-9 } ?: chapter.firstOrNull()
            return page?.locations?.position
        }

        fun positionOf(locator: Locator, ready: ReaderUiState.Ready): ReaderPosition {
            val tocIndex = currentTocIndex(ready.tableOfContents, locator.href.toString(), ready.readingOrder)
            val progression = (locator.locations.totalProgression ?: 0.0).coerceIn(0.0, 1.0)
            return ReaderPosition(
                page = locator.locations.position,
                pageCount = ready.pageCount,
                // Rounded the same way as everywhere else in the app, so the reader and Book
                // Details always show the same number.
                percent = floor(progression * 100 + 1e-9).toInt(),
                chapterTitle = tocIndex?.let { ready.tableOfContents[it].title } ?: locator.title,
                tocIndex = tocIndex,
            )
        }
    }
}
