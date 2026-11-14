package my.naqibrafiqi.pustaka.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.local.entity.HighlightEntity
import my.naqibrafiqi.pustaka.data.local.entity.ReadingProgressEntity
import my.naqibrafiqi.pustaka.data.local.entity.ReadingSessionEntity
import my.naqibrafiqi.pustaka.data.local.entity.SeriesEntity
import my.naqibrafiqi.pustaka.data.local.toEntity
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.Creator
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import my.naqibrafiqi.pustaka.testing.T0
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DaoTest {

    private lateinit var db: PustakaDatabase
    private lateinit var seriesDao: SeriesDao
    private lateinit var bookDao: BookDao

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        seriesDao = db.seriesDao()
        bookDao = db.bookDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertSeries(name: String, hidden: Boolean = false): Long = seriesDao.insert(
        SeriesEntity(
            folderPath = "/storage/emulated/0/Pustaka/$name",
            folderName = name,
            editedName = null,
            source = null,
            edited = null,
            isHidden = hidden,
            createdAt = T0,
            updatedAt = T0,
        )
    )

    private suspend fun insertBook(fileName: String, seriesId: Long? = null, addedAt: java.time.Instant = T0): Long =
        bookDao.insert(book(fileName, seriesId = seriesId, folder = "/lib/${seriesId ?: "root"}", addedAt = addedAt).toEntity())

    // --- Series ---

    @Test
    fun seriesRoundTripWithDetails() = runTest {
        val id = seriesDao.insert(
            SeriesEntity(
                folderPath = "/lib/Death Games",
                folderName = "Death Games",
                editedName = "Playing Death Games",
                source = SeriesDetails(author = "Yushi Ukai", tags = listOf("Horror", "Thriller")),
                edited = SeriesDetails(synopsis = "Mine"),
                isHidden = false,
                createdAt = T0,
                updatedAt = T0,
            )
        )
        val loaded = seriesDao.getById(id)!!
        assertEquals("Playing Death Games", loaded.editedName)
        assertEquals(SeriesDetails(author = "Yushi Ukai", tags = listOf("Horror", "Thriller")), loaded.source)
        assertEquals(SeriesDetails(synopsis = "Mine"), loaded.edited)
        assertEquals(id, seriesDao.getByFolderPath("/lib/Death Games")?.id)
    }

    @Test
    fun embeddedDetailsWithNoValuesComeBackAsNull() = runTest {
        val id = insertSeries("Empty")
        assertNull(seriesDao.getById(id)!!.source)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun folderPathsAreUnique() = runTest {
        insertSeries("Same")
        insertSeries("Same")
    }

    @Test
    fun hiddenSeriesAreListedSeparately() = runTest {
        val visible = insertSeries("Visible")
        val hidden = insertSeries("Hidden")
        seriesDao.setHidden(hidden, true, T0)

        assertEquals(listOf(visible), seriesDao.observeVisible().first().map { it.id })
        assertEquals(listOf(hidden), seriesDao.observeHidden().first().map { it.id })
    }

    @Test
    fun deletingASeriesDeletesItsBooks() = runTest {
        val series = insertSeries("Series")
        val volume = insertBook("Vol 1.epub", series)
        val standalone = insertBook("Standalone.epub")

        seriesDao.deleteById(series)

        assertNull(bookDao.getById(volume))
        assertEquals(listOf(standalone), bookDao.getAll().map { it.id })
    }

    // --- Books ---

    @Test
    fun bookRoundTripWithDetails() = runTest {
        val details = BookDetails(
            title = "Playing Death Games to Put Food on the Table, Volume 1",
            creators = listOf(Creator("Yushi Ukai", "aut"), Creator("Nekometaru", "ill")),
            subjects = listOf("Horror"),
            volumeNumber = 1.0,
        )
        val id = bookDao.insert(book("Vol 1.epub").copy(sourceDetails = details, editedName = "Mine").toEntity())
        val loaded = bookDao.getById(id)!!
        assertEquals(details, loaded.source)
        assertNull(loaded.edited)
        assertEquals("Mine", loaded.editedName)
        assertEquals(ReadingStatus.UNREAD, loaded.readingStatus)
    }

    @Test
    fun standaloneBooksAndVolumesAreSeparate() = runTest {
        val series = insertSeries("Series")
        val volume = insertBook("Vol 1.epub", series)
        val standalone = insertBook("Standalone.epub")

        assertEquals(listOf(volume), bookDao.observeVisibleInSeries(series).first().map { it.id })
        assertEquals(listOf(standalone), bookDao.observeVisibleStandalone().first().map { it.id })
        assertEquals(setOf(volume, standalone), bookDao.observeVisible().first().map { it.id }.toSet())
    }

    @Test
    fun hiddenBooksAndBooksInHiddenSeriesAreLeftOutOfTheLibrary() = runTest {
        val shownSeries = insertSeries("Shown")
        val hiddenSeries = insertSeries("Hidden", hidden = true)
        val shown = insertBook("Vol 1.epub", shownSeries)
        val inHiddenSeries = insertBook("Vol 1.epub", hiddenSeries)
        val hiddenBook = insertBook("Hidden.epub")
        bookDao.setHidden(hiddenBook, true)

        assertEquals(listOf(shown), bookDao.observeVisible().first().map { it.id })
        assertEquals(listOf(shown), bookDao.observeRecentlyAdded(10).first().map { it.id })
        assertEquals(listOf(hiddenBook), bookDao.observeHidden().first().map { it.id })
        // The series' own list still has its books, for the Hidden items screen.
        assertEquals(listOf(inHiddenSeries), bookDao.observeVisibleInSeries(hiddenSeries).first().map { it.id })
    }

    @Test
    fun missingBooksStayInTheLibrary() = runTest {
        val id = insertBook("Gone.epub")
        bookDao.setMissing(id, true)
        assertTrue(bookDao.observeVisible().first().single().isMissing)
    }

    @Test
    fun recentlyAddedIsNewestFirstAndLimited() = runTest {
        val old = insertBook("Old.epub", addedAt = T0)
        val newer = insertBook("Newer.epub", addedAt = T0.plusSeconds(60))
        val newest = insertBook("Newest.epub", addedAt = T0.plusSeconds(120))

        assertEquals(listOf(newest, newer), bookDao.observeRecentlyAdded(2).first().map { it.id })
        assertEquals(listOf(newest, newer, old), bookDao.observeRecentlyAdded(10).first().map { it.id })
    }

    @Test
    fun currentlyReadingIsMostRecentlyOpenedFirst() = runTest {
        val first = insertBook("First.epub")
        val second = insertBook("Second.epub")
        insertBook("Unread.epub")
        listOf(first, second).forEach { bookDao.setReadingStatus(it, ReadingStatus.READING) }
        bookDao.setLastOpened(first, T0.plusSeconds(10))
        bookDao.setLastOpened(second, T0.plusSeconds(20))

        assertEquals(
            listOf(second, first),
            bookDao.observeVisibleWithStatus(ReadingStatus.READING).first().map { it.id },
        )
    }

    @Test
    fun booksCanBeFoundByPathAndFingerprint() = runTest {
        val id = insertBook("Vol 1.epub")
        val entity = bookDao.getById(id)!!
        assertEquals(id, bookDao.getByFilePath(entity.filePath)?.id)
        assertEquals(listOf(id), bookDao.getByFingerprint(entity.fingerprint).map { it.id })
        assertTrue(bookDao.getByFingerprint("unknown").isEmpty())
    }

    @Test(expected = SQLiteConstraintException::class)
    fun aBookMustBelongToAnExistingSeries() = runTest {
        insertBook("Orphan.epub", seriesId = 999)
    }

    // --- Progress, sessions and highlights ---

    @Test
    fun progressIsUpsertedPerBook() = runTest {
        val bookId = insertBook("Vol 1.epub")
        val dao = db.readingProgressDao()
        dao.upsert(ReadingProgressEntity(bookId, "{\"a\":1}", 0.25, 50, T0))
        dao.upsert(ReadingProgressEntity(bookId, "{\"a\":2}", 0.58, 116, T0.plusSeconds(5)))

        val progress = dao.observe(bookId).first()!!
        assertEquals(0.58, progress.progression, 0.0)
        assertEquals(116, progress.position)
    }

    @Test
    fun sessionsAreNewestFirst() = runTest {
        val bookId = insertBook("Vol 1.epub")
        val dao = db.readingSessionDao()
        val older = dao.insert(ReadingSessionEntity(bookId = bookId, startedAt = T0, endedAt = T0.plusSeconds(600), pagesRead = 10))
        val newer = dao.insert(
            ReadingSessionEntity(bookId = bookId, startedAt = T0.plusSeconds(3600), endedAt = T0.plusSeconds(4200), pagesRead = 12)
        )
        assertEquals(listOf(newer, older), dao.observeForBook(bookId).first().map { it.id })
    }

    @Test
    fun deletingABookDeletesItsProgressSessionsAndHighlights() = runTest {
        val bookId = insertBook("Vol 1.epub")
        db.readingProgressDao().upsert(ReadingProgressEntity(bookId, "{}", 0.1, 1, T0))
        db.readingSessionDao().insert(ReadingSessionEntity(bookId = bookId, startedAt = T0, endedAt = T0, pagesRead = 0))
        db.highlightDao().insert(
            HighlightEntity(bookId = bookId, locator = "{}", text = "Quote", note = null, color = 0, createdAt = T0, updatedAt = T0)
        )

        bookDao.deleteById(bookId)

        assertNull(db.readingProgressDao().get(bookId))
        assertTrue(db.readingSessionDao().observeForBook(bookId).first().isEmpty())
        assertTrue(db.highlightDao().observeAll().first().isEmpty())
    }
}
