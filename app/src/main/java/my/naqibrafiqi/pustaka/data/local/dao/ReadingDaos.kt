package my.naqibrafiqi.pustaka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import my.naqibrafiqi.pustaka.data.local.entity.HighlightEntity
import my.naqibrafiqi.pustaka.data.local.entity.ReadingProgressEntity
import my.naqibrafiqi.pustaka.data.local.entity.ReadingSessionEntity

@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    fun observe(bookId: Long): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    suspend fun get(bookId: Long): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress")
    fun observeAll(): Flow<List<ReadingProgressEntity>>

    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun delete(bookId: Long)
}

@Dao
interface ReadingSessionDao {

    /** Newest first. */
    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startedAt DESC, id DESC")
    fun observeForBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Insert
    suspend fun insert(session: ReadingSessionEntity): Long
}

@Dao
interface HighlightDao {

    /** Newest first. */
    @Query("SELECT * FROM highlights ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAt ASC, id ASC")
    fun observeForBook(bookId: Long): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE id = :id")
    suspend fun getById(id: Long): HighlightEntity?

    /** The colour of the highlight made or changed most recently, or null when there are none. */
    @Query("SELECT color FROM highlights ORDER BY updatedAt DESC, id DESC LIMIT 1")
    suspend fun lastUsedColor(): Int?

    @Insert
    suspend fun insert(highlight: HighlightEntity): Long

    @Update
    suspend fun update(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteById(id: Long)
}
