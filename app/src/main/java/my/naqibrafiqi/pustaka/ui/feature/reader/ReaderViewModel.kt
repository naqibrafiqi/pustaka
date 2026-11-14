package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.naqibrafiqi.pustaka.data.library.epub.OpenFailure
import my.naqibrafiqi.pustaka.data.library.epub.OpenResult
import my.naqibrafiqi.pustaka.data.library.epub.ReadiumToolkit
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import java.io.File
import javax.inject.Inject
import kotlin.math.floor

sealed interface ReaderUiState {

    data object Loading : ReaderUiState

    data class Failed(val message: String) : ReaderUiState

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
    val pageLabel: String? get() = page?.let { "Page $it of $pageCount" }
}

/**
 * Opens a book in the reader and keeps its reading position saved. The position is written shortly
 * after each page turn, and straight away when the reader goes into the background.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val books: BookRepository,
    private val reading: ReadingRepository,
    private val toolkit: ReadiumToolkit,
) : ViewModel() {

    private val bookId: Long = checkNotNull(savedStateHandle[ReaderActivity.EXTRA_BOOK_ID]) {
        "The reader needs a book id."
    }
    private val fromStart: Boolean = savedStateHandle[ReaderActivity.EXTRA_FROM_START] ?: false

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val _position = MutableStateFlow(ReaderPosition())
    val position: StateFlow<ReaderPosition> = _position.asStateFlow()

    private var publication: Publication? = null

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
        _position.value = positionOf(locator, ready)
    }

    /** Saves the position now, for when the reader goes into the background or closes. */
    fun saveNow() {
        val locator = latestLocator.value ?: return
        viewModelScope.launch { save(locator) }
    }

    private suspend fun open() {
        val book = books.getBook(bookId)
        if (book == null) return fail("This book is no longer in your library.")
        if (book.isMissing) return fail("The file for this book is missing. It may have been moved or deleted.")

        val opened = when (val result = toolkit.open(File(book.filePath))) {
            is OpenResult.Failed -> return fail(messageFor(result.reason))
            is OpenResult.Opened -> result.publication
        }
        publication = opened

        if (fromStart) reading.clearProgress(bookId)
        val saved = if (fromStart) null else reading.observeProgress(bookId).first()
        val initialLocator = saved?.locator?.let(::parseLocator)

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

    private fun fail(message: String) {
        _state.value = ReaderUiState.Failed(message)
    }

    override fun onCleared() {
        publication?.close()
        publication = null
    }

    companion object {
        const val SAVE_DELAY_MS = 500L

        fun messageFor(reason: OpenFailure): String = when (reason) {
            OpenFailure.NOT_READABLE -> "Pustaka can't read this file. Check that it's still in your library folder."
            OpenFailure.NOT_EPUB -> "This file isn't a valid EPUB, or it's damaged."
            OpenFailure.PROTECTED -> "This book is protected with DRM, so Pustaka can't open it."
        }

        /** A saved locator that can't be read any more just means starting from the beginning. */
        fun parseLocator(json: String): Locator? =
            runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()

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
