package my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.ReadingSession
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.ui.model.BookState
import my.naqibrafiqi.pustaka.ui.model.ReadingStats
import my.naqibrafiqi.pustaka.ui.model.SeriesProgress
import my.naqibrafiqi.pustaka.ui.model.creditLine
import my.naqibrafiqi.pustaka.ui.model.fileSizeLabel
import my.naqibrafiqi.pustaka.ui.model.pageLabel
import my.naqibrafiqi.pustaka.ui.model.pagesLabel
import my.naqibrafiqi.pustaka.ui.model.percentRead
import my.naqibrafiqi.pustaka.ui.model.timeLeftLabel
import my.naqibrafiqi.pustaka.ui.model.velocityLabel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
