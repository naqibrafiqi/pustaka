package my.naqibrafiqi.pustaka.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.testing.TestClock
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookmarkRepositoryTest {

    private lateinit var db: PustakaDatabase
    private val clock = TestClock()
    private lateinit var books: BookRepository
    private lateinit var bookmarks: BookmarkRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        books = DefaultBookRepository(db, clock)
        bookmarks = DefaultBookmarkRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun bookmarksAreListedInBookOrderForTheirBookOnly() = runTest {
        val bookId = books.addBook(book("a.epub"))
        val other = books.addBook(book("b.epub"))
        bookmarks.addBookmark(bookId, locator = "{\"late\":1}", progression = 0.8)
        clock.advanceSeconds(1)
        bookmarks.addBookmark(bookId, locator = "{\"early\":1}", progression = 0.2, text = "It began")
        bookmarks.addBookmark(other, locator = "{}", progression = 0.5)

        val listed = bookmarks.observeBookmarks(bookId).first()

        assertEquals(listOf(0.2, 0.8), listed.map { it.progression })
        assertEquals(clock.instant(), listed.first().createdAt)
        assertEquals("It began", listed.first().text)
    }

    @Test
    fun bookmarksCanBeDeletedAndGoWithTheirBook() = runTest {
        val bookId = books.addBook(book("a.epub"))
        val first = bookmarks.addBookmark(bookId, locator = "{}", progression = 0.1)
        bookmarks.addBookmark(bookId, locator = "{}", progression = 0.2)

        bookmarks.deleteBookmarks(listOf(first))
        assertEquals(listOf(0.2), bookmarks.observeBookmarks(bookId).first().map { it.progression })

        books.deleteBook(bookId)
        assertTrue(bookmarks.observeBookmarks(bookId).first().isEmpty())
    }
}
