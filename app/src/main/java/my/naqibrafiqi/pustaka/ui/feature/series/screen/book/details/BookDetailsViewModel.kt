package my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.data.library.DeleteResult
import my.naqibrafiqi.pustaka.data.library.LibraryFiles
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.ReadingSession
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.ui.model.BookState
import my.naqibrafiqi.pustaka.ui.model.DetailsEvent
import my.naqibrafiqi.pustaka.ui.model.ReadingStats
import my.naqibrafiqi.pustaka.ui.model.SeriesProgress
import my.naqibrafiqi.pustaka.ui.model.creditLine
import my.naqibrafiqi.pustaka.ui.model.fileSizeLabel
import my.naqibrafiqi.pustaka.ui.model.pageLabel
import my.naqibrafiqi.pustaka.ui.model.pagesLabel
import my.naqibrafiqi.pustaka.ui.model.percentRead
import my.naqibrafiqi.pustaka.ui.model.timeLeftLabel
import my.naqibrafiqi.pustaka.ui.model.velocityLabel
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The steps of "Delete book". See "Deleting" in `docs/local_storage_and_library.md`. */
sealed interface BookDeleteDialog {
    val name: String
    val fileName: String

    /** "Remove from library" or "Delete file". */
    data class Choose(override val name: String, override val fileName: String) : BookDeleteDialog

    /** The last check before the file is deleted. */
    data class ConfirmDeleteFile(override val name: String, override val fileName: String) : BookDeleteDialog

    /** The file has gone already, so only the book's data is left to remove. */
    data class ConfirmForget(override val name: String, override val fileName: String) : BookDeleteDialog
}

/** One line in the "More info" dialog. */
data class InfoRow(val label: String, val value: String)

sealed interface BookDetailsUiState {

    data object Loading : BookDetailsUiState

    data class Error(val message: String) : BookDetailsUiState

    /** The book was deleted, for example because it was removed for good while this was open. */
    data object NotFound : BookDetailsUiState

    data class Ready(
        val title: String,
        /** "Playing Death Games · Volume 1" for a volume, or null for a standalone book. */
        val seriesLine: String?,
        val credits: String?,
        val coverPath: String?,
        /** The book's subjects. */
        val genres: List<String>,
        val synopsis: String?,
        val state: BookState,
        val percent: Int,
        /** "Page 116 of 200", or null when nothing is known yet. */
        val pageLabel: String?,
        /** The page to continue from, or null when the book hasn't been started. */
        val continuePage: Int?,
        val pageCount: String,
        val velocity: String,
        val timeLeft: String,
        val info: List<InfoRow>,
    ) : BookDetailsUiState {
        val canRead: Boolean get() = state != BookState.MISSING
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = BookDetailsViewModel.Factory::class)
class BookDetailsViewModel @AssistedInject constructor(
    @Assisted private val bookId: Long,
    private val books: BookRepository,
    private val reading: ReadingRepository,
    private val seriesRepository: SeriesRepository,
    private val libraryFiles: LibraryFiles,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(bookId: Long): BookDetailsViewModel
    }

    private val attempt = MutableStateFlow(0)

    val state: StateFlow<BookDetailsUiState> = attempt
        .flatMapLatest {
            val book = books.observeBook(bookId)
            combine(
                book,
                book.map { it?.seriesId }.distinctUntilChanged().flatMapLatest(::seriesLine),
                reading.observeProgress(bookId),
                reading.observeSessions(bookId),
            ) { current, seriesLine, progress, sessions ->
                if (current == null) BookDetailsUiState.NotFound else buildState(current, seriesLine, progress, sessions)
            }.catch { emit(BookDetailsUiState.Error("Couldn't load this book.")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BookDetailsUiState.Loading)

    private val _deleteDialog = MutableStateFlow<BookDeleteDialog?>(null)

    /** The delete dialog that's showing, if any. */
    val deleteDialog: StateFlow<BookDeleteDialog?> = _deleteDialog.asStateFlow()

    private val _events = Channel<DetailsEvent>(Channel.BUFFERED)
    val events: Flow<DetailsEvent> = _events.receiveAsFlow()

    /**
     * "Delete book". A book whose file has gone can only be removed for good. Otherwise the user
     * picks between removing it from the library and deleting its file.
     */
    fun requestDelete() {
        viewModelScope.launch {
            val book = books.getBook(bookId) ?: return@launch
            val fileName = File(book.filePath).name
            _deleteDialog.value = if (book.isMissing) {
                BookDeleteDialog.ConfirmForget(book.name.value, fileName)
            } else {
                BookDeleteDialog.Choose(book.name.value, fileName)
            }
        }
    }

    fun chooseDeleteFile() {
        val choose = _deleteDialog.value as? BookDeleteDialog.Choose ?: return
        _deleteDialog.value = BookDeleteDialog.ConfirmDeleteFile(choose.name, choose.fileName)
    }

    /** Hides the book. Its file isn't touched. */
    fun removeFromLibrary() = finish { books.setHidden(bookId, true) }

    /** Deletes the book's data. Only offered once its file has gone. */
    fun forget() = finish { books.deleteBook(bookId) }

    /** Deletes the file, after the user has confirmed. */
    fun deleteFile() {
        _deleteDialog.value = null
        viewModelScope.launch {
            when (val result = libraryFiles.deleteBookFile(bookId)) {
                DeleteResult.Deleted -> _events.send(DetailsEvent.Closed)
                is DeleteResult.Failed -> _events.send(DetailsEvent.Message(result.message))
            }
        }
    }

    fun dismissDeleteDialog() {
        _deleteDialog.value = null
    }

    private fun finish(action: suspend () -> Unit) {
        _deleteDialog.value = null
        viewModelScope.launch {
            action()
            _events.send(DetailsEvent.Closed)
        }
    }

    fun retry() {
        attempt.value++
    }

    /** "Series name · Volume 3", which needs the whole series to number volumes without one. */
    private fun seriesLine(seriesId: Long?): Flow<String?> {
        if (seriesId == null) return flowOf(null)
        return combine(
            seriesRepository.observeSeries(seriesId),
            books.observeVolumes(seriesId),
        ) { series, volumes ->
            val volume = volumes.firstOrNull { it.id == bookId }
            listOfNotNull(series?.name?.value, volume?.let { SeriesProgress(volumes).labelOf(it) })
                .joinToString(" · ")
                .ifEmpty { null }
        }
    }

    private fun buildState(
        book: Book,
        seriesLine: String?,
        progress: ReadingProgress?,
        sessions: List<ReadingSession>,
    ): BookDetailsUiState.Ready {
        val details = book.details
        val percent = book.percentRead(progress)
        val state = BookState.of(book)
        val position = progress?.position.takeIf { state == BookState.READING }
        val stats = ReadingStats(sessions)
        val pagesLeft = book.totalPositions?.let { total -> total - (position ?: 0) }
        return BookDetailsUiState.Ready(
            title = book.name.value,
            seriesLine = seriesLine,
            credits = details.creditLine(),
            coverPath = details.coverPath,
            genres = details.subjects.orEmpty(),
            synopsis = details.description,
            state = state,
            percent = percent,
            pageLabel = pageLabel(position, book.totalPositions),
            continuePage = position,
            pageCount = book.totalPositions?.let(::pagesLabel) ?: ReadingStats.NONE,
            velocity = velocityLabel(stats.pagesPerHour),
            timeLeft = when {
                state == BookState.FINISHED -> "Finished"
                pagesLeft == null -> ReadingStats.NONE
                else -> timeLeftLabel(stats.timeLeft(pagesLeft))
            },
            info = infoRows(book, seriesLine),
        )
    }

    private fun infoRows(book: Book, seriesLine: String?): List<InfoRow> {
        val details = book.details
        return listOfNotNull(
            details.title?.let { InfoRow("Title in file", it) },
            seriesLine?.let { InfoRow("Series", it) },
            details.publisher?.let { InfoRow("Publisher", it) },
            details.publishedDate?.let { InfoRow("Published", it) },
            details.language?.let { InfoRow("Language", it) },
            details.identifier?.let { InfoRow("Identifier", it) },
            InfoRow("File", book.filePath),
            InfoRow("File size", fileSizeLabel(book.fileSize)),
            InfoRow("Added", DATE.format(book.addedAt.atZone(ZoneId.systemDefault()))),
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    }
}
