package my.naqibrafiqi.pustaka.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import my.naqibrafiqi.pustaka.data.library.NaturalOrder
import my.naqibrafiqi.pustaka.data.library.VolumeOrder
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.local.dao.BookDao
import my.naqibrafiqi.pustaka.data.local.toEntity
import my.naqibrafiqi.pustaka.data.local.toModel
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.EditRules
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import java.time.Clock
import javax.inject.Inject

/**
 * Books shown in the library are the ones that aren't hidden and aren't in a hidden series.
 * Missing books are included, so they can be shown greyed out.
 */
interface BookRepository {

    /** Every book in the library, standalone books and volumes, in no particular order. */
    fun observeLibraryBooks(): Flow<List<Book>>

    /** The volumes of a series in reading order. See [VolumeOrder]. */
    fun observeVolumes(seriesId: Long): Flow<List<Book>>

    /** Books that aren't in a series, sorted by name. */
    fun observeStandaloneBooks(): Flow<List<Book>>

    /** Newest first. */
    fun observeRecentlyAdded(limit: Int): Flow<List<Book>>

    /** Books being read, the most recently opened first. */
    fun observeCurrentlyReading(): Flow<List<Book>>

    /** Books removed from the library one by one, sorted by name. */
    fun observeHiddenBooks(): Flow<List<Book>>

    fun observeBook(id: Long): Flow<Book?>

    suspend fun getBook(id: Long): Book?

    /** Adds a book. Its [Book.id] is ignored. Returns the new id. */
    suspend fun addBook(book: Book): Long

    /**
     * Saves the values from the Edit details form. Values that are blank or match the default
     * aren't stored as edits, so those fields go back to their defaults.
     */
    suspend fun updateEdits(id: Long, name: String?, details: BookDetails)

    /** Removes all of the user's edits. */
    suspend fun resetEdits(id: Long)

    /** Hides or restores a book ("Remove from library"). Its file isn't touched. */
    suspend fun setHidden(id: Long, hidden: Boolean)

    suspend fun setReadingStatus(id: Long, status: ReadingStatus)

    /** Records that the book was opened now. */
    suspend fun markOpened(id: Long)

    /** Deletes the book, its progress, sessions, highlights and bookmarks. The file isn't touched. */
    suspend fun deleteBook(id: Long)
}

class DefaultBookRepository @Inject constructor(
    private val database: PustakaDatabase,
    private val clock: Clock,
) : BookRepository {

    private val dao: BookDao = database.bookDao()

    private val byName: Comparator<Book> =
        compareBy<Book, String>(NaturalOrder) { it.name.value }.thenBy { it.id }

    override fun observeLibraryBooks(): Flow<List<Book>> =
        dao.observeVisible().map { books -> books.map { it.toModel() } }

    override fun observeVolumes(seriesId: Long): Flow<List<Book>> =
        dao.observeVisibleInSeries(seriesId).map { books -> VolumeOrder.sort(books.map { it.toModel() }) }

    override fun observeStandaloneBooks(): Flow<List<Book>> =
        dao.observeVisibleStandalone().map { books -> books.map { it.toModel() }.sortedWith(byName) }

    override fun observeRecentlyAdded(limit: Int): Flow<List<Book>> =
        dao.observeRecentlyAdded(limit).map { books -> books.map { it.toModel() } }

    override fun observeCurrentlyReading(): Flow<List<Book>> =
        dao.observeVisibleWithStatus(ReadingStatus.READING).map { books -> books.map { it.toModel() } }

    override fun observeHiddenBooks(): Flow<List<Book>> =
        dao.observeHidden().map { books -> books.map { it.toModel() }.sortedWith(byName) }

    override fun observeBook(id: Long): Flow<Book?> = dao.observeById(id).map { it?.toModel() }

    override suspend fun getBook(id: Long): Book? = dao.getById(id)?.toModel()

    override suspend fun addBook(book: Book): Long = dao.insert(book.copy(id = 0).toEntity())

    override suspend fun updateEdits(id: Long, name: String?, details: BookDetails) =
        modify(id) { book ->
            book.copy(
                editedName = EditRules.text(name, Book.defaultNameFor(book.fileName)),
                editedDetails = details.editsAgainst(book.sourceDetails),
            )
        }

    override suspend fun resetEdits(id: Long) =
        modify(id) { it.copy(editedName = null, editedDetails = BookDetails.EMPTY) }

    override suspend fun setHidden(id: Long, hidden: Boolean) = dao.setHidden(id, hidden)

    override suspend fun setReadingStatus(id: Long, status: ReadingStatus) =
        dao.setReadingStatus(id, status)

    override suspend fun markOpened(id: Long) = dao.setLastOpened(id, clock.instant())

    override suspend fun deleteBook(id: Long) = dao.deleteById(id)

    private suspend fun modify(id: Long, change: (Book) -> Book) {
        database.withTransaction {
            val book = dao.getById(id)?.toModel() ?: return@withTransaction
            dao.update(change(book).toEntity())
        }
    }
}
