package my.naqibrafiqi.pustaka.ui.feature.highlights.screen

import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.export.ExportFiles
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.model.HighlightColor
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultBookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultHighlightRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultSeriesRepository
import my.naqibrafiqi.pustaka.data.repository.HighlightRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.testing.MainDispatcherRule
import my.naqibrafiqi.pustaka.testing.TestClock
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Against a real (in-memory) database. */
@RunWith(RobolectricTestRunner::class)
class HighlightsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private lateinit var db: PustakaDatabase
    private val clock = TestClock()
    private lateinit var books: BookRepository
    private lateinit var series: SeriesRepository
    private lateinit var highlights: HighlightRepository
    private val exportFiles = FakeExportFiles()

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        books = DefaultBookRepository(db, clock)
        series = DefaultSeriesRepository(db, clock)
        highlights = DefaultHighlightRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun highlightsAreGroupedBySeriesThenVolumeThenPlaceInTheBook() = runTest {
        val zebra = series.addSeries("/P/Zebra", "Zebra")
        val alpha = series.addSeries("/P/Alpha", "Alpha")
        val alpha2 = books.addBook(book("Alpha 2.epub", seriesId = alpha, folder = "/P/Alpha"))
        val alpha10 = books.addBook(book("Alpha 10.epub", seriesId = alpha, folder = "/P/Alpha"))
        val zebra1 = books.addBook(book("Zebra 1.epub", seriesId = zebra, folder = "/P/Zebra"))
        val standalone = books.addBook(book("A standalone.epub"))
        add(alpha10, "Ten", progression = 0.5)
        add(standalone, "Alone", progression = 0.1)
        add(alpha2, "Later in two", progression = 0.8, page = 80)
        add(zebra1, "Zebra quote", progression = 0.2)
        add(alpha2, "Early in two", progression = 0.1, page = 10)

        val state = viewModel().ready()

        assertEquals(listOf("Alpha", "Zebra", null), state.groups.map { it.name })
        assertEquals(listOf("Alpha 2", "Alpha 10"), state.groups[0].books.map { it.title })
        assertEquals(listOf("Early in two", "Later in two"), state.groups[0].books[0].highlights.map { it.highlight.text })
        assertEquals(listOf(10, 80), state.groups[0].books[0].highlights.map { it.page })
        assertEquals(5, state.totalCount)
    }

    @Test
    fun highlightsInBooksRemovedFromTheLibraryArentShown() = runTest {
        val kept = books.addBook(book("Kept.epub"))
        val removed = books.addBook(book("Removed.epub"))
        add(kept, "Kept quote")
        add(removed, "Removed quote")
        books.setHidden(removed, true)

        val state = viewModel().ready()

        assertEquals(listOf("Kept"), state.groups.single().books.map { it.title })
        assertEquals(1, state.totalCount)
    }

    @Test
    fun searchLooksAtTheTextNotesAndBookTitles() = runTest {
        val brunhild = books.addBook(book("Brunhild.epub"))
        val divas = books.addBook(book("Battle Divas.epub"))
        add(brunhild, "The dragon's maiden")
        add(brunhild, "A quiet morning", note = "Lovely DRAGON imagery")
        add(brunhild, "Nothing here")
        add(divas, "A Valkyrie descended")
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.search("dragon")
        val byText = viewModel.ready { it.query == "dragon" }
        assertEquals(listOf("The dragon's maiden", "A quiet morning"), byText.groups.flatMap { g -> g.books.flatMap { b -> b.highlights.map { it.highlight.text } } })
        assertEquals(4, byText.totalCount)

        viewModel.search("battle divas")
        val byTitle = viewModel.ready { it.query == "battle divas" }
        assertEquals(listOf("Battle Divas"), byTitle.groups.flatMap { g -> g.books.map { it.title } })

        viewModel.search("nowhere")
        assertTrue(viewModel.ready { it.query == "nowhere" }.groups.isEmpty())
    }

    @Test
    fun aHighlightCanBeChangedAndDeleted() = runTest {
        val bookId = books.addBook(book("Vol 1.epub"))
        val id = add(bookId, "A quote")
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.openHighlight(id)
        viewModel.updateHighlight(id, HighlightColor.PINK, note = " Good ")
        val changed = highlights.observeAllHighlights().first { it.single().color == HighlightColor.PINK }.single()
        assertEquals("Good", changed.note)

        viewModel.deleteHighlight(id)
        assertNull(viewModel.openHighlightId.value)
        assertTrue(highlights.observeAllHighlights().first { it.isEmpty() }.isEmpty())
    }

    @Test
    fun exportingOneBookSharesAMarkdownFileOfItsHighlights() = runTest {
        val one = books.addBook(book("Brunhild, Vol. 01.epub"))
        val other = books.addBook(book("Other.epub"))
        add(one, "First quote", page = 3)
        add(other, "Not exported")
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.exportBook(one)

        val share = viewModel.events.first() as HighlightsEvent.Share
        assertEquals("Brunhild, Vol. 01 highlights.md", share.fileName)
        val text = exportFiles.written.getValue(share.fileName)
        assertTrue(text, text.contains("> First quote"))
        assertTrue(text, !text.contains("Not exported"))
    }

    @Test
    fun aFileThatCantBeWrittenSaysSo() = runTest {
        add(books.addBook(book("One.epub")), "First quote")
        exportFiles.fails = true
        val viewModel = viewModel()
        viewModel.ready()

        viewModel.exportAll()

        assertEquals(HighlightsEvent.Message("Couldn't export your highlights."), viewModel.events.first())
    }

    @Test
    fun exportingEverythingIgnoresTheSearch() = runTest {
        val one = books.addBook(book("One.epub"))
        val two = books.addBook(book("Two.epub"))
        add(one, "First quote")
        add(two, "Second quote")
        val viewModel = viewModel()
        viewModel.ready()
        viewModel.search("first")
        viewModel.ready { it.query == "first" }

        viewModel.exportAll()

        val share = viewModel.events.first() as HighlightsEvent.Share
        assertEquals("Pustaka highlights.md", share.fileName)
        val text = exportFiles.written.getValue(share.fileName)
        assertTrue(text, text.contains("> First quote") && text.contains("> Second quote"))
    }

    private var created = 0L

    /** Adds a highlight at a place in the book, with a locator like the reader saves. */
    private suspend fun add(
        bookId: Long,
        text: String,
        progression: Double = 0.0,
        page: Int? = null,
        note: String? = null,
    ): Long {
        val positionJson = page?.let { ""","position":$it""" }.orEmpty()
        val locator = """{"href":"chapter.xhtml","type":"application/xhtml+xml","locations":{"totalProgression":$progression$positionJson},"text":{"highlight":"$text"}}"""
        clock.advanceSeconds(++created)
        return highlights.addHighlight(bookId, locator = locator, text = text, note = note, color = HighlightColor.YELLOW)
    }

    private fun viewModel() = HighlightsViewModel(
        highlights = highlights,
        books = books,
        series = series,
        exportFiles = exportFiles,
        clock = clock,
    )

    private suspend fun HighlightsViewModel.ready(
        predicate: (HighlightsUiState.Ready) -> Boolean = { true },
    ): HighlightsUiState.Ready =
        state.first { it is HighlightsUiState.Ready && predicate(it) } as HighlightsUiState.Ready
}

/** Keeps what would be written, by file name. */
private class FakeExportFiles : ExportFiles {
    val written = mutableMapOf<String, String>()
    var fails = false

    override suspend fun write(fileName: String, text: String): Uri? {
        if (fails) return null
        written[fileName] = text
        return Uri.parse("content://test/$fileName")
    }
}
