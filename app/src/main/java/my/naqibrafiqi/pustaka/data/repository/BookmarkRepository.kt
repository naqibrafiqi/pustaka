package my.naqibrafiqi.pustaka.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.local.toEntity
import my.naqibrafiqi.pustaka.data.local.toModel
import my.naqibrafiqi.pustaka.data.model.Bookmark
import java.time.Clock
import javax.inject.Inject

/**
 * Bookmarked pages, added with the reader's bookmark button and listed in its Contents sheet (#37,
 * D27) and in the Highlights tab (#51, D33).
 */
interface BookmarkRepository {

    /** In book order. */
    fun observeBookmarks(bookId: Long): Flow<List<Bookmark>>

    /** Every bookmark in the library, each book's in book order. */
    fun observeAllBookmarks(): Flow<List<Bookmark>>

    /**
     * @param locator where the page starts, as a serialized Readium `Locator`.
     * @param progression how far into the book, from 0 to 1.
     * @param text the start of the page's text, to show in the list.
     */
    suspend fun addBookmark(bookId: Long, locator: String, progression: Double, text: String? = null): Long

    suspend fun deleteBookmarks(ids: List<Long>)
}

class DefaultBookmarkRepository @Inject constructor(
    database: PustakaDatabase,
    private val clock: Clock,
) : BookmarkRepository {

    private val dao = database.bookmarkDao()

    override fun observeBookmarks(bookId: Long): Flow<List<Bookmark>> =
        dao.observeForBook(bookId).map { bookmarks -> bookmarks.map { it.toModel() } }

    override fun observeAllBookmarks(): Flow<List<Bookmark>> =
        dao.observeAll().map { bookmarks -> bookmarks.map { it.toModel() } }

    override suspend fun addBookmark(bookId: Long, locator: String, progression: Double, text: String?): Long =
        dao.insert(
            Bookmark(
                bookId = bookId,
                locator = locator,
                progression = progression.coerceIn(0.0, 1.0),
                text = text,
                createdAt = clock.instant(),
            ).toEntity()
        )

    override suspend fun deleteBookmarks(ids: List<Long>) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids)
    }
}
