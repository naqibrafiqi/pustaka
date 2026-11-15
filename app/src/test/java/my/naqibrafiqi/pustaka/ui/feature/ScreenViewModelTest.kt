package my.naqibrafiqi.pustaka.ui.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.library.LibraryFiles
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.Creator
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultBookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultReadingRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultSeriesRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.testing.FakeScans
import my.naqibrafiqi.pustaka.testing.FakeSettings
import my.naqibrafiqi.pustaka.testing.MainDispatcherRule
import my.naqibrafiqi.pustaka.testing.T0
import my.naqibrafiqi.pustaka.testing.TestClock
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
import my.naqibrafiqi.pustaka.testing.string
import my.naqibrafiqi.pustaka.testing.text
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryFilter
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryUiState
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDetailsUiState
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDetailsViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDetailsUiState
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDetailsViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.SeriesUiState
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.SeriesViewModel
import my.naqibrafiqi.pustaka.ui.model.BookState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The four screens' ViewModels, reading from a real (in-memory) database. */
@RunWith(RobolectricTestRunner::class)
class ScreenViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private lateinit var db: PustakaDatabase
    private val clock = TestClock()
    private lateinit var series: SeriesRepository
    private lateinit var books: BookRepository
    private lateinit var reading: ReadingRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        series = DefaultSeriesRepository(db, clock)
        books = DefaultBookRepository(db, clock)
        reading = DefaultReadingRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    private fun files() = LibraryFiles(FakeSettings(), series, books, FakeScans())

    private fun library() = LibraryViewModel(books, reading, FakeSettings(), clock, SavedStateHandle())

    private suspend inline fun <reified S> StateFlow<*>.awaitState(): S = first { it is S } as S

    private suspend fun addSeriesWithVolumes(name: String, volumes: Int): Pair<Long, List<Long>> {
        val seriesId = series.addSeries("/lib/$name", name, SeriesDetails(author = "Yushi Ukai", mainGenre = "Action"))
        val ids = (1..volumes).map { n ->
            books.addBook(
                book("$name, Vol. 0$n.epub", seriesId = seriesId, folder = "/lib/$name", addedAt = T0.plusSeconds(n.toLong()))
                    .copy(fileSize = 1024L * 1024, totalPositions = 200)
            )
        }
        return seriesId to ids
    }

    // --- Library ---

    @Test
    fun libraryShowsEveryBookNewestFirstWithFilterCounts() = runTest {
        val standalone = books.addBook(book("The Left Hand of Darkness.epub", addedAt = T0.plusSeconds(100)))
        val (_, volumes) = addSeriesWithVolumes("Death Games", 2)
        books.setReadingStatus(volumes[0], ReadingStatus.READING)

        val state = library().state.awaitState<LibraryUiState.Ready>()

        assertEquals(listOf(standalone, volumes[1], volumes[0]), state.books.map { it.id })
        assertEquals(mapOf(LibraryFilter.ALL to 3, LibraryFilter.READING to 1, LibraryFilter.UNREAD to 2), state.counts)
        assertEquals("Volume 1", state.books.last().volumeLabel?.text())
        assertNull(state.books.first().volumeLabel)
    }

    @Test
    fun libraryFiltersBooks() = runTest {
        books.addBook(book("a.epub"))
        val reading = books.addBook(book("b.epub"))
        books.setReadingStatus(reading, ReadingStatus.READING)
        val viewModel = library()
        viewModel.state.awaitState<LibraryUiState.Ready>()

        viewModel.selectFilter(LibraryFilter.READING)

        val state = viewModel.state.first { it is LibraryUiState.Ready && it.filter == LibraryFilter.READING }
        assertEquals(listOf(reading), (state as LibraryUiState.Ready).books.map { it.id })
    }

    @Test
    fun currentlyReadingIsTheBookOpenedLast() = runTest {
        val older = books.addBook(book("older.epub").copy(totalPositions = 312))
        val newer = books.addBook(book("newer.epub").copy(totalPositions = 312))
        listOf(older, newer).forEach { books.setReadingStatus(it, ReadingStatus.READING) }
        books.markOpened(older)
        clock.advanceSeconds(60)
        books.markOpened(newer)
        reading.saveProgress(newer, locator = "{}", progression = 0.68, position = 214)
        clock.advanceSeconds(32 * 60)

        val current = library().state.awaitState<LibraryUiState.Ready>().currentlyReading!!

        assertEquals(newer, current.book.id)
        assertEquals(68, current.book.percent)
        assertEquals("Page 214 of 312", current.pageLabel?.text())
        assertEquals("Read 32m ago", current.readAgo?.text())
    }

    @Test
    fun anEmptyLibraryIsReadyWithNoBooks() = runTest {
        val state = library().state.awaitState<LibraryUiState.Ready>()

        assertTrue(state.isLibraryEmpty)
        assertNull(state.currentlyReading)
        assertEquals("/storage/emulated/0/Pustaka", state.libraryFolderPath)
    }

    @Test
    fun libraryLeavesOutHiddenBooksAndSeries() = runTest {
        val hiddenBook = books.addBook(book("hidden.epub"))
        books.setHidden(hiddenBook, true)
        val (hiddenSeries, _) = addSeriesWithVolumes("Gone", 2)
        series.setHidden(hiddenSeries, true)
        val shown = books.addBook(book("shown.epub"))

        assertEquals(listOf(shown), library().state.awaitState<LibraryUiState.Ready>().books.map { it.id })
    }

    // --- Series ---

    @Test
    fun seriesTabListsSeriesButNotStandaloneBooks() = runTest {
        books.addBook(book("Standalone.epub"))
        val (seriesId, volumes) = addSeriesWithVolumes("Death Games", 3)
        series.addSeries("/lib/Empty", "Empty")
        books.setReadingStatus(volumes[0], ReadingStatus.FINISHED)

        val state = SeriesViewModel(series, books, FakeSettings(), files()).state.awaitState<SeriesUiState.Ready>()

        assertEquals(listOf("Death Games", "Empty"), state.series.map { it.name })
        val deathGames = state.series.first { it.id == seriesId }
        assertEquals(3, deathGames.volumeCount)
        assertEquals(1, deathGames.finishedCount)
        assertEquals(33, deathGames.percent)
        assertEquals(volumes[1], deathGames.continueBookId)
        assertEquals("Up next: Volume 2", deathGames.currentLabel.text())
        assertEquals(listOf("Action"), deathGames.genres)

        val empty = state.series.first { it.name == "Empty" }
        assertEquals(0, empty.volumeCount)
        assertNull(empty.continueBookId)
    }

    // --- Series Details ---

    @Test
    fun seriesDetailsCountsIndexedFilesAndTheirSize() = runTest {
        val (seriesId, volumes) = addSeriesWithVolumes("Death Games", 3)
        db.bookDao().setMissing(volumes[2], true)

        val state = SeriesDetailsViewModel(seriesId, series, books, reading, files(), SavedStateHandle())
            .state.awaitState<SeriesDetailsUiState.Ready>()

        assertEquals("Death Games", state.name)
        assertEquals("Yushi Ukai", state.author)
        assertEquals(3, state.volumeCount)
        assertEquals(2, state.indexedCount)
        assertEquals("2.0 MB", state.totalSize)
        assertEquals("/lib/Death Games", state.folderPath)
        assertEquals(volumes, state.volumes.map { it.id })
        assertEquals(BookState.MISSING, state.volumes.last().state)
        assertEquals(listOf("Volume 1", "Volume 2", "Volume 3"), state.volumes.map { it.volumeLabel?.text() })
    }

    @Test
    fun seriesDetailsTogglesBetweenGridAndList() = runTest {
        val (seriesId, _) = addSeriesWithVolumes("Death Games", 1)
        val viewModel = SeriesDetailsViewModel(seriesId, series, books, reading, files(), SavedStateHandle())
        assertTrue(viewModel.state.awaitState<SeriesDetailsUiState.Ready>().isGrid)

        viewModel.toggleLayout()

        assertFalse((viewModel.state.first { it is SeriesDetailsUiState.Ready && !it.isGrid } as SeriesDetailsUiState.Ready).isGrid)
    }

    @Test
    fun seriesDetailsForAnUnknownSeriesIsNotFound() = runTest {
        val state = SeriesDetailsViewModel(404, series, books, reading, files(), SavedStateHandle()).state
            .first { it !is SeriesDetailsUiState.Loading }

        assertEquals(SeriesDetailsUiState.NotFound, state)
    }

    // --- Book Details ---

    @Test
    fun bookDetailsShowsTheBooksDetailsProgressAndStats() = runTest {
        val (_, volumes) = addSeriesWithVolumes("Death Games", 2)
        val id = volumes[1]
        books.updateEdits(
            id,
            name = null,
            details = BookDetails(
                creators = listOf(Creator("Yushi Ukai"), Creator("Nekometaru", Creator.ROLE_ILLUSTRATOR)),
                description = "Yuki wakes up in a manor.",
                subjects = listOf("Thriller", "Drama"),
                publisher = "Seven Seas",
            ),
        )
        books.setReadingStatus(id, ReadingStatus.READING)
        reading.saveProgress(id, locator = "{}", progression = 0.5, position = 100)
        reading.recordSession(id, startedAt = T0, endedAt = T0.plusSeconds(3600), pagesRead = 40)

        val state = BookDetailsViewModel(id, books, reading, series, files())
            .state.awaitState<BookDetailsUiState.Ready>()

        assertEquals("Death Games, Vol. 02", state.title)
        assertEquals("Death Games · Volume 2", state.seriesLine?.text())
        assertEquals("Yushi Ukai, Nekometaru", state.credits)
        assertEquals(listOf("Thriller", "Drama"), state.genres)
        assertEquals(BookState.READING, state.state)
        assertEquals(50, state.percent)
        assertEquals("Page 100 of 200", state.pageLabel?.text())
        assertEquals(100, state.continuePage)
        assertEquals("200 pages", state.pageCount.text())
        assertEquals("Avg. 40 pages/h", state.velocity.text())
        assertEquals("~2h 30m", state.timeLeft.text())
        assertTrue(state.info.any { string(it.label) == "Publisher" && it.value.text() == "Seven Seas" })
    }

    @Test
    fun bookDetailsOfAnUnreadStandaloneBookHasNoSeriesOrStats() = runTest {
        val id = books.addBook(book("The Left Hand of Darkness.epub"))

        val state = BookDetailsViewModel(id, books, reading, series, files())
            .state.awaitState<BookDetailsUiState.Ready>()

        assertNull(state.seriesLine)
        assertNull(state.continuePage)
        assertEquals(BookState.UNREAD, state.state)
        assertEquals("—", state.pageCount.text())
        assertEquals("—", state.velocity.text())
        assertEquals("—", state.timeLeft.text())
    }

    @Test
    fun bookDetailsNoticesWhenTheBookIsDeleted() = runTest {
        val id = books.addBook(book("a.epub"))
        val viewModel = BookDetailsViewModel(id, books, reading, series, files())
        viewModel.state.awaitState<BookDetailsUiState.Ready>()

        books.deleteBook(id)

        assertEquals(BookDetailsUiState.NotFound, viewModel.state.first { it == BookDetailsUiState.NotFound })
    }
}
