package my.naqibrafiqi.pustaka.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.local.toEntity
import my.naqibrafiqi.pustaka.data.local.toModel
import my.naqibrafiqi.pustaka.data.model.Highlight
import java.time.Clock
import javax.inject.Inject

/** Highlights. The table is ready now; the reader and Highlights tab use it from Phase 5. */
interface HighlightRepository {

    /** Newest first. */
    fun observeAllHighlights(): Flow<List<Highlight>>

    /** In the order they were made. */
    fun observeHighlights(bookId: Long): Flow<List<Highlight>>

    suspend fun addHighlight(bookId: Long, locator: String, text: String, note: String?, color: Int): Long

    suspend fun updateHighlight(id: Long, note: String?, color: Int)

    suspend fun deleteHighlight(id: Long)
}

class DefaultHighlightRepository @Inject constructor(
    private val database: PustakaDatabase,
    private val clock: Clock,
) : HighlightRepository {

    private val dao = database.highlightDao()

    override fun observeAllHighlights(): Flow<List<Highlight>> =
        dao.observeAll().map { highlights -> highlights.map { it.toModel() } }

    override fun observeHighlights(bookId: Long): Flow<List<Highlight>> =
        dao.observeForBook(bookId).map { highlights -> highlights.map { it.toModel() } }

    override suspend fun addHighlight(
        bookId: Long,
        locator: String,
        text: String,
        note: String?,
        color: Int,
    ): Long {
        val now = clock.instant()
        return dao.insert(
            Highlight(
                bookId = bookId,
                locator = locator,
                text = text,
                note = note?.trim()?.ifEmpty { null },
                color = color,
                createdAt = now,
                updatedAt = now,
            ).toEntity()
        )
    }

    override suspend fun updateHighlight(id: Long, note: String?, color: Int) {
        database.withTransaction {
            val highlight = dao.getById(id)?.toModel() ?: return@withTransaction
            dao.update(
                highlight.copy(
                    note = note?.trim()?.ifEmpty { null },
                    color = color,
                    updatedAt = clock.instant(),
                ).toEntity()
            )
        }
    }

    override suspend fun deleteHighlight(id: Long) = dao.deleteById(id)
}
