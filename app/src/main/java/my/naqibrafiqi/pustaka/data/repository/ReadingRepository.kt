package my.naqibrafiqi.pustaka.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.local.entity.ReadingProgressEntity
import my.naqibrafiqi.pustaka.data.local.entity.ReadingSessionEntity
import my.naqibrafiqi.pustaka.data.local.toModel
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.ReadingSession
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Reading positions and reading sessions. The reader writes these from Phase 4. */
interface ReadingRepository {

    fun observeProgress(bookId: Long): Flow<ReadingProgress?>

    /** The reading position of every book that has one, by book id. */
    fun observeAllProgress(): Flow<Map<Long, ReadingProgress>>

    /**
     * Saves where the reader is.
     *
     * @param locator a serialized Readium `Locator`.
     * @param progression how far through the book, from 0.0 to 1.0. Values outside are clamped.
     */
    suspend fun saveProgress(bookId: Long, locator: String, progression: Double, position: Int?)

    /** Forgets the reading position, for "Read From Beginning" or "Mark as unread". */
    suspend fun clearProgress(bookId: Long)

    /** Newest first. */
    fun observeSessions(bookId: Long): Flow<List<ReadingSession>>

    suspend fun recordSession(bookId: Long, startedAt: Instant, endedAt: Instant, pagesRead: Int): Long
}

class DefaultReadingRepository @Inject constructor(
    database: PustakaDatabase,
    private val clock: Clock,
) : ReadingRepository {

    private val progressDao = database.readingProgressDao()
    private val sessionDao = database.readingSessionDao()

    override fun observeProgress(bookId: Long): Flow<ReadingProgress?> =
        progressDao.observe(bookId).map { it?.toModel() }

    override fun observeAllProgress(): Flow<Map<Long, ReadingProgress>> =
        progressDao.observeAll().map { rows -> rows.associate { it.bookId to it.toModel() } }

    override suspend fun saveProgress(
        bookId: Long,
        locator: String,
        progression: Double,
        position: Int?,
    ) = progressDao.upsert(
        ReadingProgressEntity(
            bookId = bookId,
            locator = locator,
            progression = progression.coerceIn(0.0, 1.0),
            position = position,
            updatedAt = clock.instant(),
        )
    )

    override suspend fun clearProgress(bookId: Long) = progressDao.delete(bookId)

    override fun observeSessions(bookId: Long): Flow<List<ReadingSession>> =
        sessionDao.observeForBook(bookId).map { sessions -> sessions.map { it.toModel() } }

    override suspend fun recordSession(
        bookId: Long,
        startedAt: Instant,
        endedAt: Instant,
        pagesRead: Int,
    ): Long {
        require(!endedAt.isBefore(startedAt)) { "A session can't end before it starts." }
        return sessionDao.insert(
            ReadingSessionEntity(
                bookId = bookId,
                startedAt = startedAt,
                endedAt = endedAt,
                pagesRead = pagesRead.coerceAtLeast(0),
            )
        )
    }
}
