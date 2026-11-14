package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.library.epub.OpenFailure
import my.naqibrafiqi.pustaka.data.library.epub.ReadiumToolkit
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultBookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultReadingRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import my.naqibrafiqi.pustaka.testing.MainDispatcherRule
import my.naqibrafiqi.pustaka.testing.T0
import my.naqibrafiqi.pustaka.testing.TestClock
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
import my.naqibrafiqi.pustaka.testing.writeEpub
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.random.Random

/** Opens real (small) EPUB files with Readium, against a real (in-memory) database. */
@RunWith(RobolectricTestRunner::class)
class ReaderViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var db: PustakaDatabase
    private val clock = TestClock()
    private lateinit var books: BookRepository
    private lateinit var reading: ReadingRepository
    private val toolkit = ReadiumToolkit(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        books = DefaultBookRepository(db, clock)
        reading = DefaultReadingRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun opensAnUnreadBookAtTheStartAndMarksItBeingRead() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        clock.advanceSeconds(60)

        val ready = viewModel(bookId).ready()

        assertEquals("Brunhild, Vol. 01", ready.title)
        assertNull(ready.initialLocator)
        assertTrue("A test book has several pages", ready.pageCount > 1)
        assertEquals(listOf("Chapter 1"), ready.tableOfContents.map { it.title })
        val book = checkNotNull(books.getBook(bookId))
        assertEquals(ReadingStatus.READING, book.readingStatus)
        assertEquals(T0.plusSeconds(60), book.lastOpenedAt)
    }

    @Test
    fun savesThePositionAndResumesFromIt() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val first = viewModel(bookId)
        val page3 = first.ready().positions[2]

        first.onLocatorChanged(page3)
        first.saveNow()

        val saved = reading.observeProgress(bookId).first { it != null }!!
        assertEquals(3, saved.position)
        assertEquals(page3.locations.totalProgression!!, saved.progression, 1e-9)

        val resumed = viewModel(bookId).ready()
        assertEquals(page3.href, resumed.initialLocator?.href)
        assertEquals(3, resumed.initialLocator?.locations?.position)
    }

    @Test
    fun readingFromTheBeginningForgetsThePosition() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        reading.saveProgress(bookId, locator = "{}", progression = 0.5, position = 4)
        books.setReadingStatus(bookId, ReadingStatus.FINISHED)

        val ready = viewModel(bookId, fromStart = true).ready()

        assertNull(ready.initialLocator)
        assertNull(reading.observeProgress(bookId).first())
        // Reading a finished book again makes it a book being read.
        assertEquals(ReadingStatus.READING, books.getBook(bookId)?.readingStatus)
    }

    @Test
    fun continuingAFinishedBookKeepsItFinished() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        books.setReadingStatus(bookId, ReadingStatus.FINISHED)

        viewModel(bookId).ready()

        assertEquals(ReadingStatus.FINISHED, books.getBook(bookId)?.readingStatus)
    }

    @Test
    fun aSavedPositionThatCantBeReadStartsFromTheBeginning() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        reading.saveProgress(bookId, locator = "not json", progression = 0.5, position = 4)

        assertNull(viewModel(bookId).ready().initialLocator)
    }

    @Test
    fun theProgressBarFollowsThePage() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val viewModel = viewModel(bookId)
        val ready = viewModel.ready()

        val lastPage = ready.positions.last()
        viewModel.onLocatorChanged(lastPage)

        val position = viewModel.position.value
        assertEquals(ready.pageCount, position.page)
        // The same number Book Details shows for the saved position.
        val saved = ReadingProgress(bookId, "{}", lastPage.locations.totalProgression!!, ready.pageCount, T0)
        assertEquals(saved.percent, position.percent)
        assertEquals("Chapter 1", position.chapterTitle)
        assertEquals(0, position.tocIndex)
        assertEquals("Page ${ready.pageCount} of ${ready.pageCount}", position.pageLabel)
    }

    @Test
    fun aMissingBookCantBeOpened() = runTest {
        val bookId = books.addBook(book("Gone.epub").copy(isMissing = true))

        val failed = viewModel(bookId).failed()

        assertTrue(failed.message.contains("missing"))
    }

    @Test
    fun aDamagedFileCantBeOpened() = runTest {
        val file = temp.root.resolve("Broken.epub").apply { writeText("not an epub") }
        val bookId = addBook(file)

        assertEquals(ReaderViewModel.messageFor(OpenFailure.NOT_EPUB), viewModel(bookId).failed().message)
        // Nothing was opened, so nothing changes.
        assertEquals(ReadingStatus.UNREAD, books.getBook(bookId)?.readingStatus)
    }

    @Test
    fun aDeletedBookCantBeOpened() = runTest {
        assertNotNull(viewModel(bookId = 404).failed().message)
    }

    /**
     * Readium counts pages from each chapter's size inside the zip, so the text is varied enough
     * not to compress away, giving the book several pages.
     */
    private fun epub(name: String): File {
        val random = Random(seed = 1)
        val words = List(3_000) { List(random.nextInt(2, 9)) { 'a' + random.nextInt(26) }.joinToString("") }
        return writeEpub(temp.root.resolve(name), metadata = "<dc:title>Test</dc:title>", chapterText = words.joinToString(" "))
    }

    private suspend fun addBook(file: File): Long =
        books.addBook(book(file.name, folder = file.parent))

    private fun viewModel(bookId: Long, fromStart: Boolean = false) = ReaderViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(ReaderActivity.EXTRA_BOOK_ID to bookId, ReaderActivity.EXTRA_FROM_START to fromStart),
        ),
        books = books,
        reading = reading,
        toolkit = toolkit,
    )

    private suspend fun ReaderViewModel.ready(): ReaderUiState.Ready =
        state.first { it !is ReaderUiState.Loading } as ReaderUiState.Ready

    private suspend fun ReaderViewModel.failed(): ReaderUiState.Failed =
        state.first { it !is ReaderUiState.Loading } as ReaderUiState.Failed
}
