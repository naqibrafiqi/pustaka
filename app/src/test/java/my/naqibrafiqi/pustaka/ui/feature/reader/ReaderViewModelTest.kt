package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.library.epub.OpenFailure
import my.naqibrafiqi.pustaka.data.library.epub.ReadiumToolkit
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.model.HighlightColor
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultBookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultHighlightRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultReadingRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultSeriesRepository
import my.naqibrafiqi.pustaka.data.repository.HighlightRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.data.settings.ReaderSettings
import my.naqibrafiqi.pustaka.data.settings.ReaderSettingsRepository
import my.naqibrafiqi.pustaka.data.settings.ReaderTheme
import my.naqibrafiqi.pustaka.testing.MainDispatcherRule
import my.naqibrafiqi.pustaka.testing.T0
import my.naqibrafiqi.pustaka.testing.TestClock
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
import my.naqibrafiqi.pustaka.testing.string
import my.naqibrafiqi.pustaka.testing.text
import my.naqibrafiqi.pustaka.testing.writeEpub
import my.naqibrafiqi.pustaka.util.text.uiText
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
import org.readium.r2.shared.publication.Locator
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.Duration
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
    private lateinit var series: SeriesRepository
    private lateinit var highlights: HighlightRepository
    private val readerSettings = FakeReaderSettings()
    private val toolkit = ReadiumToolkit(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        books = DefaultBookRepository(db, clock)
        reading = DefaultReadingRepository(db, clock)
        series = DefaultSeriesRepository(db, clock)
        highlights = DefaultHighlightRepository(db, clock)
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
        assertEquals("Page ${ready.pageCount} of ${ready.pageCount}", position.pageLabel?.text())
    }

    @Test
    fun aMissingBookCantBeOpened() = runTest {
        val bookId = books.addBook(book("Gone.epub").copy(isMissing = true))

        val failed = viewModel(bookId).failed()

        assertTrue(string(failed.message).contains("missing"))
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

    @Test
    fun recordsASessionWhenTheReaderGoesIntoTheBackground() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val viewModel = viewModel(bookId)
        val pages = viewModel.ready().positions
        val started = clock.now

        viewModel.onLocatorChanged(pages[0])
        clock.advanceSeconds(60)
        viewModel.onLocatorChanged(pages[1])
        clock.advanceSeconds(60)
        viewModel.onLocatorChanged(pages[2])
        clock.advanceSeconds(30)
        viewModel.onReaderPaused()

        val session = reading.observeSessions(bookId).first { it.isNotEmpty() }.single()
        assertEquals(2, session.pagesRead)
        assertEquals(started, session.startedAt)
        assertEquals(started.plusSeconds(150), session.endedAt)
        // "Read 32m ago" counts from when the reading stopped.
        books.observeBook(bookId).first { it?.lastOpenedAt == started.plusSeconds(150) }
    }

    @Test
    fun comingBackStartsANewSessionOnTheSamePage() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val viewModel = viewModel(bookId)
        val pages = viewModel.ready().positions
        viewModel.onLocatorChanged(pages[0])
        clock.advanceSeconds(60)
        viewModel.onReaderPaused()

        clock.advanceSeconds(3_600)
        viewModel.onReaderResumed()
        clock.advanceSeconds(60)
        viewModel.onLocatorChanged(pages[1])
        viewModel.onReaderPaused()

        // The first stretch turned no pages, so only the second one is recorded.
        val session = reading.observeSessions(bookId).first { it.isNotEmpty() }.single()
        assertEquals(1, session.pagesRead)
        assertEquals(Duration.ofSeconds(60), session.duration)
    }

    @Test
    fun reachingTheLastPageFinishesTheBookAndOffersTheNextVolume() = runTest {
        val seriesId = series.addSeries(temp.root.resolve("Brunhild").path, "Brunhild", SeriesDetails())
        val first = addBook(epub("Brunhild/Brunhild, Vol. 01.epub"), seriesId)
        val second = addBook(epub("Brunhild/Brunhild, Vol. 02.epub"), seriesId)
        val viewModel = viewModel(first)
        val ready = viewModel.ready()

        viewModel.onLocatorChanged(ready.positions[ready.pageCount - 2])
        assertNull(viewModel.finishCard.value)
        viewModel.onLocatorChanged(ready.positions.last())

        val card = viewModel.card()
        assertEquals("Brunhild, Vol. 01", card.title)
        assertEquals(NextVolume(second, "Brunhild, Vol. 02", uiText(R.string.volume_label, "2"), fromStart = false), card.next)
        assertEquals(ReadingStatus.FINISHED, books.getBook(first)?.readingStatus)

        // Turning back and forth on the last page doesn't show it again once closed.
        viewModel.dismissFinishCard()
        viewModel.onLocatorChanged(ready.positions[ready.pageCount - 2])
        viewModel.onLocatorChanged(ready.positions.last())
        assertNull(viewModel.finishCard.value)
    }

    @Test
    fun aFinishedNextVolumeIsReadAgainFromTheStart() = runTest {
        val seriesId = series.addSeries(temp.root.resolve("Brunhild").path, "Brunhild", SeriesDetails())
        val first = addBook(epub("Brunhild/Brunhild, Vol. 01.epub"), seriesId)
        val second = addBook(epub("Brunhild/Brunhild, Vol. 02.epub"), seriesId)
        books.setReadingStatus(second, ReadingStatus.FINISHED)
        val viewModel = viewModel(first)

        viewModel.onLocatorChanged(viewModel.ready().positions.last())

        assertEquals(true, viewModel.card().next?.fromStart)
    }

    @Test
    fun theLastVolumeHasNothingNext() = runTest {
        val seriesId = series.addSeries(temp.root.resolve("Brunhild").path, "Brunhild", SeriesDetails())
        val last = addBook(epub("Brunhild/Brunhild, Vol. 01.epub"), seriesId)
        val viewModel = viewModel(last)

        viewModel.onLocatorChanged(viewModel.ready().positions.last())

        val card = viewModel.card()
        assertNull(card.next)
        assertEquals("Brunhild", card.seriesName)
    }

    @Test
    fun aStandaloneBookIsJustFinished() = runTest {
        val bookId = addBook(epub("Battle Divas, Vol. 01.epub"))
        val viewModel = viewModel(bookId)

        viewModel.onLocatorChanged(viewModel.ready().positions.last())

        val card = viewModel.card()
        assertNull(card.next)
        assertNull(card.seriesName)
        assertEquals(ReadingStatus.FINISHED, books.getBook(bookId)?.readingStatus)
    }

    @Test
    fun settingsAreLoadedBeforeTheBookIsShownAndSavedWhenChanged() = runTest {
        readerSettings.current.value = ReaderSettings(theme = ReaderTheme.SEPIA)
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val viewModel = viewModel(bookId)
        viewModel.ready()

        assertEquals(ReaderTheme.SEPIA, viewModel.settings.value.theme)

        viewModel.updateSettings(viewModel.settings.value.copy(scroll = true))
        assertEquals(true, readerSettings.current.value.scroll)

        viewModel.resetSettings()
        assertEquals(ReaderSettings(), readerSettings.current.value)
        assertEquals(ReaderSettings(), viewModel.settings.value)
    }

    @Test
    fun selectedTextIsHighlightedInTheColourUsedLast() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val other = books.addBook(book("Other.epub"))
        highlights.addHighlight(other, locator = "{}", text = "Elsewhere", note = null, color = HighlightColor.BLUE)
        val viewModel = viewModel(bookId)
        val page = viewModel.ready().positions[1]

        viewModel.addHighlight(page.copy(text = Locator.Text(before = "the ", highlight = " a quote ", after = " here")))

        val highlight = viewModel.highlights.first { it.isNotEmpty() }.single()
        assertEquals("a quote", highlight.highlight.text)
        assertEquals(HighlightColor.BLUE, highlight.highlight.color)
        // The text around it is kept, so the reader can find the passage again.
        assertEquals("the ", highlight.locator.text.before)
        assertEquals(page.href, highlight.locator.href)
    }

    @Test
    fun opensAtAHighlightInsteadOfTheSavedPosition() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val first = viewModel(bookId)
        val positions = first.ready().positions
        first.onLocatorChanged(positions[0])
        first.saveNow()
        val highlight = positions.last().copy(text = Locator.Text(highlight = "a quote"))

        val ready = viewModel(bookId, locator = highlight.toJSON().toString()).ready()

        assertEquals(highlight.href, ready.initialLocator?.href)
        assertEquals(highlight.locations.position, ready.initialLocator?.locations?.position)
        assertEquals("a quote", ready.initialLocator?.text?.highlight)
    }

    @Test
    fun aJumpOffersThePageItLeft() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val viewModel = viewModel(bookId)
        val positions = viewModel.ready().positions
        viewModel.onLocatorChanged(positions[1])
        assertNull(viewModel.returnPoint.value)

        viewModel.onJump(positions.last())
        viewModel.onLocatorChanged(positions.last())

        assertEquals(2, viewModel.returnPoint.value?.page)
        assertEquals(positions[1], viewModel.takeReturnPoint())
        assertNull(viewModel.returnPoint.value)
    }

    @Test
    fun aJumpToAChapterStartFindsItsPage() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val ready = viewModel(bookId).ready()
        val chapterStart = ready.publication.locatorFromLink(ready.tableOfContents.single().link)!!

        assertNull(chapterStart.locations.position)
        assertEquals(1, ReaderViewModel.pageOf(chapterStart, ready))
        val page3 = ready.positions[2]
        assertEquals(3, ReaderViewModel.pageOf(page3.copy(locations = page3.locations.copy(position = null)), ready))
    }

    @Test
    fun openingAtAHighlightOffersTheSavedPage() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val first = viewModel(bookId)
        val positions = first.ready().positions
        first.onLocatorChanged(positions[1])
        first.saveNow()
        reading.observeProgress(bookId).first { it != null }
        val highlight = positions.last().copy(text = Locator.Text(highlight = "a quote"))

        val atHighlight = viewModel(bookId, locator = highlight.toJSON().toString())
        atHighlight.ready()

        assertEquals(2, atHighlight.returnPoint.value?.page)
        // Opening at the saved page itself offers nothing.
        val atSaved = viewModel(bookId, locator = positions[1].toJSON().toString())
        atSaved.ready()
        assertNull(atSaved.returnPoint.value)
    }

    @Test
    fun aSelectionWithoutTextIsntHighlighted() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val viewModel = viewModel(bookId)
        val page = viewModel.ready().positions[0]

        viewModel.addHighlight(page.copy(text = Locator.Text(highlight = "   ")))
        viewModel.addHighlight(page)

        assertTrue(highlights.observeHighlights(bookId).first().isEmpty())
    }

    @Test
    fun aTappedHighlightCanBeChangedAndDeleted() = runTest {
        val bookId = addBook(epub("Brunhild, Vol. 01.epub"))
        val viewModel = viewModel(bookId)
        val page = viewModel.ready().positions[0]
        viewModel.addHighlight(page.copy(text = Locator.Text(highlight = "a quote")))
        val id = viewModel.highlights.first { it.isNotEmpty() }.single().id

        viewModel.openHighlight(id)
        assertEquals(id, viewModel.openHighlightId.value)

        viewModel.updateHighlight(id, HighlightColor.PINK, note = "  Good line ")
        val changed = viewModel.highlights.first { it.single().highlight.color == HighlightColor.PINK }.single()
        assertEquals("Good line", changed.highlight.note)

        viewModel.deleteHighlight(id)
        assertNull(viewModel.openHighlightId.value)
        assertTrue(viewModel.highlights.first { it.isEmpty() }.isEmpty())
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

    private suspend fun addBook(file: File, seriesId: Long? = null): Long =
        books.addBook(book(file.name, seriesId = seriesId, folder = file.parent))

    private fun viewModel(bookId: Long, fromStart: Boolean = false, locator: String? = null) = ReaderViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                ReaderActivity.EXTRA_BOOK_ID to bookId,
                ReaderActivity.EXTRA_FROM_START to fromStart,
                ReaderActivity.EXTRA_LOCATOR to locator,
            ),
        ),
        books = books,
        reading = reading,
        toolkit = toolkit,
        seriesRepository = series,
        readerSettings = readerSettings,
        highlightRepository = highlights,
        clock = clock,
    )

    private suspend fun ReaderViewModel.ready(): ReaderUiState.Ready =
        state.first { it !is ReaderUiState.Loading } as ReaderUiState.Ready

    private suspend fun ReaderViewModel.failed(): ReaderUiState.Failed =
        state.first { it !is ReaderUiState.Loading } as ReaderUiState.Failed

    private suspend fun ReaderViewModel.card(): FinishCard = finishCard.filterNotNull().first()
}

private class FakeReaderSettings : ReaderSettingsRepository {
    val current = MutableStateFlow(ReaderSettings())
    override val settings: Flow<ReaderSettings> = current
    override suspend fun update(settings: ReaderSettings) {
        current.value = settings
    }
    override suspend fun reset() {
        current.value = ReaderSettings()
    }
}
