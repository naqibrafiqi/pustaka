package my.naqibrafiqi.pustaka.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.model.HighlightColor
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import my.naqibrafiqi.pustaka.testing.T0
import my.naqibrafiqi.pustaka.testing.TestClock
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoryTest {

    private lateinit var db: PustakaDatabase
    private val clock = TestClock()
    private lateinit var series: SeriesRepository
    private lateinit var books: BookRepository
    private lateinit var reading: ReadingRepository
    private lateinit var highlights: HighlightRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        series = DefaultSeriesRepository(db, clock)
        books = DefaultBookRepository(db, clock)
        reading = DefaultReadingRepository(db, clock)
        highlights = DefaultHighlightRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    // --- Series ---

    @Test
    fun seriesAreSortedByShownNameWith10After9() = runTest {
        series.addSeries("/lib/b", "Series 10")
        series.addSeries("/lib/a", "Series 9")
        val renamed = series.addSeries("/lib/c", "zzz")
        series.updateEdits(renamed, name = "Alpha", details = SeriesDetails.EMPTY)

        assertEquals(listOf("Alpha", "Series 9", "Series 10"), series.observeSeries().first().map { it.name.value })
    }

    @Test
    fun seriesEditsOnlyKeepWhatChanged() = runTest {
        val id = series.addSeries("/lib/dg", "Death Games", SeriesDetails(author = "Yushi Ukai", synopsis = "From file"))
        clock.advanceSeconds(60)

        series.updateEdits(
            id,
            name = "Death Games",
            details = SeriesDetails(author = "Yushi Ukai", synopsis = "Mine", mainGenre = "Horror"),
        )

        val saved = series.getSeries(id)!!
        assertNull(saved.editedName)
        assertEquals(SeriesDetails(synopsis = "Mine", mainGenre = "Horror"), saved.editedDetails)
        assertEquals("Mine", saved.details.synopsis)
        assertEquals("Yushi Ukai", saved.details.author)
        assertEquals(T0.plusSeconds(60), saved.updatedAt)
        assertEquals(T0, saved.createdAt)
    }

    @Test
    fun editedSeriesFieldsSurviveANewDefaultFromTheFile() = runTest {
        val id = series.addSeries("/lib/dg", "Death Games", SeriesDetails(author = "Old", synopsis = "Old synopsis"))
        series.updateEdits(id, name = null, details = SeriesDetails(author = "Old", synopsis = "Mine"))

        series.updateSourceDetails(id, SeriesDetails(author = "New", synopsis = "New synopsis"))

        val details = series.getSeries(id)!!.details
        assertEquals("New", details.author) // Not edited, so it follows the file.
        assertEquals("Mine", details.synopsis) // Edited, so it's kept.
    }

    @Test
    fun resettingSeriesEditsGoesBackToTheDefaults() = runTest {
        val id = series.addSeries("/lib/dg", "Death Games", SeriesDetails(author = "Yushi Ukai"))
        series.updateEdits(id, name = "Mine", details = SeriesDetails(author = "Someone"))

        series.resetEdits(id)

        val reset = series.getSeries(id)!!
        assertEquals("Death Games", reset.name.value)
        assertEquals("Yushi Ukai", reset.details.author)
        assertFalse(reset.name.isEdited)
    }

    @Test
    fun hidingASeriesHidesItsBooksFromTheLibrary() = runTest {
        val id = series.addSeries("/lib/dg", "Death Games")
        books.addBook(book("Vol 1.epub", seriesId = id))
        books.addBook(book("Standalone.epub"))

        series.setHidden(id, true)

        assertTrue(series.observeSeries().first().isEmpty())
        assertEquals(listOf("Death Games"), series.observeHiddenSeries().first().map { it.name.value })
        assertEquals(listOf("Standalone.epub"), books.observeLibraryBooks().first().map { it.fileName })

        series.setHidden(id, false)
        assertEquals(2, books.observeLibraryBooks().first().size)
    }

    // --- Books ---

    @Test
    fun volumesComeInVolumeOrder() = runTest {
        val id = series.addSeries("/lib/dg", "Death Games")
        listOf("Vol 10.epub", "Vol 2.epub", "Vol 1.epub").forEach { books.addBook(book(it, seriesId = id)) }
        val moved = books.addBook(book("Vol 9.epub", seriesId = id))
        books.updateEdits(moved, name = null, details = BookDetails(volumeNumber = 0.5))

        assertEquals(
            listOf("Vol 9.epub", "Vol 1.epub", "Vol 2.epub", "Vol 10.epub"),
            books.observeVolumes(id).first().map { it.fileName },
        )
    }

    @Test
    fun standaloneBooksAreSortedByShownName() = runTest {
        books.addBook(book("b 10.epub"))
        books.addBook(book("B 9.epub"))
        val renamed = books.addBook(book("zzz.epub"))
        books.updateEdits(renamed, name = "A book", details = BookDetails.EMPTY)

        assertEquals(listOf("A book", "B 9", "b 10"), books.observeStandaloneBooks().first().map { it.name.value })
    }

    @Test
    fun bookEditsOnlyKeepWhatChangedAndCanBeReset() = runTest {
        val fromFile = BookDetails(title = "Title in file", publisher = "Yen On", volumeNumber = 1.0)
        val id = books.addBook(book("Vol 1.epub").copy(sourceDetails = fromFile))

        books.updateEdits(
            id,
            name = "  Playing Death Games 1 ",
            details = fromFile.copy(title = "My title", publisher = "", volumeNumber = 1.0),
        )

        val edited = books.getBook(id)!!
        assertEquals("Playing Death Games 1", edited.name.value)
        assertEquals(BookDetails(title = "My title"), edited.editedDetails)
        assertEquals("Yen On", edited.details.publisher) // Cleared in the form, so back to the file's value.

        books.resetEdits(id)
        val reset = books.getBook(id)!!
        assertEquals("Vol 1", reset.name.value)
        assertEquals(fromFile, reset.details)
    }

    @Test
    fun hidingAndRestoringABook() = runTest {
        val id = books.addBook(book("Vol 1.epub"))
        books.setHidden(id, true)
        assertTrue(books.observeLibraryBooks().first().isEmpty())
        assertEquals(listOf(id), books.observeHiddenBooks().first().map { it.id })

        books.setHidden(id, false)
        assertEquals(listOf(id), books.observeLibraryBooks().first().map { it.id })
    }

    @Test
    fun openingABookPutsItAtTheTopOfCurrentlyReading() = runTest {
        val first = books.addBook(book("First.epub"))
        val second = books.addBook(book("Second.epub"))
        listOf(first, second).forEach { books.setReadingStatus(it, ReadingStatus.READING) }

        books.markOpened(second)
        clock.advanceSeconds(30)
        books.markOpened(first)

        assertEquals(listOf(first, second), books.observeCurrentlyReading().first().map { it.id })
        assertEquals(T0.plusSeconds(30), books.getBook(first)!!.lastOpenedAt)
    }

    @Test
    fun observingABookSeesChanges() = runTest {
        val id = books.addBook(book("Vol 1.epub"))
        assertEquals(ReadingStatus.UNREAD, books.observeBook(id).first()!!.readingStatus)
        books.setReadingStatus(id, ReadingStatus.FINISHED)
        assertEquals(ReadingStatus.FINISHED, books.observeBook(id).first()!!.readingStatus)
        books.deleteBook(id)
        assertNull(books.observeBook(id).first())
    }

    // --- Reading progress and sessions ---

    @Test
    fun progressIsSavedClampedAndCleared() = runTest {
        val id = books.addBook(book("Vol 1.epub"))

        reading.saveProgress(id, locator = "{}", progression = 0.58, position = 116)
        assertEquals(58, reading.observeProgress(id).first()!!.percent)

        reading.saveProgress(id, locator = "{}", progression = 0.999, position = 199)
        assertEquals(99, reading.observeProgress(id).first()!!.percent)

        reading.saveProgress(id, locator = "{}", progression = 1.2, position = 200)
        assertEquals(100, reading.observeProgress(id).first()!!.percent)
        assertEquals(1.0, reading.observeProgress(id).first()!!.progression, 0.0)

        reading.clearProgress(id)
        assertNull(reading.observeProgress(id).first())
    }

    @Test
    fun sessionsAreRecorded() = runTest {
        val id = books.addBook(book("Vol 1.epub"))
        reading.recordSession(id, startedAt = T0, endedAt = T0.plusSeconds(1800), pagesRead = 20)

        val session = reading.observeSessions(id).first().single()
        assertEquals(30, session.duration.toMinutes())
        assertEquals(20, session.pagesRead)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aSessionCantEndBeforeItStarts() = runTest {
        val id = books.addBook(book("Vol 1.epub"))
        reading.recordSession(id, startedAt = T0, endedAt = T0.minusSeconds(1), pagesRead = 1)
    }

    // --- Highlights ---

    @Test
    fun highlightsCanBeAddedEditedAndDeleted() = runTest {
        val bookId = books.addBook(book("Vol 1.epub"))
        val id = highlights.addHighlight(bookId, locator = "{}", text = "A quote", note = "  ", color = HighlightColor.GREEN)
        assertNull(highlights.observeHighlights(bookId).first().single().note)

        clock.advanceSeconds(5)
        highlights.updateHighlight(id, note = "Good line", color = HighlightColor.BLUE)
        val updated = highlights.observeAllHighlights().first().single()
        assertEquals("Good line", updated.note)
        assertEquals(HighlightColor.BLUE, updated.color)
        assertEquals(T0.plusSeconds(5), updated.updatedAt)

        highlights.deleteHighlight(id)
        assertTrue(highlights.observeHighlights(bookId).first().isEmpty())
    }

    @Test
    fun aNewHighlightGetsTheColourUsedLast() = runTest {
        val bookId = books.addBook(book("Vol 1.epub"))
        assertEquals(HighlightColor.YELLOW, highlights.lastUsedColor())

        val first = highlights.addHighlight(bookId, locator = "{}", text = "One", note = null, color = HighlightColor.GREEN)
        clock.advanceSeconds(1)
        highlights.addHighlight(bookId, locator = "{}", text = "Two", note = null, color = HighlightColor.PINK)
        assertEquals(HighlightColor.PINK, highlights.lastUsedColor())

        // Changing an older highlight counts as using its colour.
        clock.advanceSeconds(1)
        highlights.updateHighlight(first, note = null, color = HighlightColor.BLUE)
        assertEquals(HighlightColor.BLUE, highlights.lastUsedColor())
    }
}
