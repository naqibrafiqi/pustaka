package my.naqibrafiqi.pustaka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import my.naqibrafiqi.pustaka.data.local.entity.BookEntity
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import java.time.Instant

/**
 * "Visible" books are the ones shown in the library: not hidden themselves, and not in a hidden
 * series. Missing books are still visible (they're shown greyed out).
 */
@Dao
interface BookDao {

    @Query(
        """
        SELECT books.* FROM books
        LEFT JOIN series ON series.id = books.seriesId
        WHERE books.isHidden = 0 AND (books.seriesId IS NULL OR series.isHidden = 0)
        """
    )
    fun observeVisible(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE seriesId = :seriesId AND isHidden = 0")
    fun observeVisibleInSeries(seriesId: Long): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE seriesId IS NULL AND isHidden = 0")
    fun observeVisibleStandalone(): Flow<List<BookEntity>>

    @Query(
        """
        SELECT books.* FROM books
        LEFT JOIN series ON series.id = books.seriesId
        WHERE books.isHidden = 0 AND (books.seriesId IS NULL OR series.isHidden = 0)
        ORDER BY books.addedAt DESC, books.id DESC
        LIMIT :limit
        """
    )
    fun observeRecentlyAdded(limit: Int): Flow<List<BookEntity>>

    @Query(
        """
        SELECT books.* FROM books
        LEFT JOIN series ON series.id = books.seriesId
        WHERE books.isHidden = 0 AND (books.seriesId IS NULL OR series.isHidden = 0)
            AND books.readingStatus = :status
        ORDER BY books.lastOpenedAt DESC, books.id DESC
        """
    )
    fun observeVisibleWithStatus(status: ReadingStatus): Flow<List<BookEntity>>

    /** Books hidden one by one. Books in a hidden series are listed with their series instead. */
    @Query("SELECT * FROM books WHERE isHidden = 1")
    fun observeHidden(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE filePath = :filePath")
    suspend fun getByFilePath(filePath: String): BookEntity?

    @Query("SELECT * FROM books WHERE fingerprint = :fingerprint")
    suspend fun getByFingerprint(fingerprint: String): List<BookEntity>

    @Query("SELECT * FROM books")
    suspend fun getAll(): List<BookEntity>

    /** Every book in the series, including hidden and missing ones. */
    @Query("SELECT * FROM books WHERE seriesId = :seriesId")
    suspend fun getInSeries(seriesId: Long): List<BookEntity>

    @Query("SELECT COUNT(*) FROM books WHERE seriesId = :seriesId")
    suspend fun countInSeries(seriesId: Long): Int

    @Insert
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Query("UPDATE books SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: Long, hidden: Boolean)

    @Query("UPDATE books SET isMissing = :missing WHERE id = :id")
    suspend fun setMissing(id: Long, missing: Boolean)

    @Query("UPDATE books SET readingStatus = :status WHERE id = :id")
    suspend fun setReadingStatus(id: Long, status: ReadingStatus)

    @Query("UPDATE books SET lastOpenedAt = :openedAt WHERE id = :id")
    suspend fun setLastOpened(id: Long, openedAt: Instant)

    /** Also deletes the book's progress, sessions and highlights. */
    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: Long)
}
