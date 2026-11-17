package my.naqibrafiqi.pustaka.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import java.time.Instant

/**
 * A series folder.
 *
 * [source] holds the details taken from the series' volumes (see SeriesDefaults), and [edited] the
 * user's edits.
 * Room sets an embedded object to null when all of its columns are null, so both are nullable.
 */
@Entity(
    tableName = "series",
    indices = [Index(value = ["folderPath"], unique = true)],
)
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderPath: String,
    val folderName: String,
    val editedName: String?,
    @Embedded(prefix = "source_") val source: SeriesDetails?,
    @Embedded(prefix = "edited_") val edited: SeriesDetails?,
    val isHidden: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * An EPUB file. [seriesId] is null for a standalone book.
 *
 * [source] holds the details read from `content.opf`, and [edited] the user's edits. Room sets an
 * embedded object to null when all of its columns are null, so both are nullable.
 */
@Entity(
    tableName = "books",
    foreignKeys = [
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["seriesId"]),
        Index(value = ["filePath"], unique = true),
        Index(value = ["fingerprint"]),
    ],
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesId: Long?,
    val filePath: String,
    val fileName: String,
    val fingerprint: String,
    val fileSize: Long,
    val fileLastModified: Instant,
    val isMissing: Boolean,
    val isHidden: Boolean,
    val editedName: String?,
    @Embedded(prefix = "source_") val source: BookDetails?,
    @Embedded(prefix = "edited_") val edited: BookDetails?,
    val totalPositions: Int?,
    val readingStatus: ReadingStatus,
    val addedAt: Instant,
    val lastOpenedAt: Instant?,
)

/** The reading position in a book. There's at most one row per book. */
@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReadingProgressEntity(
    @PrimaryKey val bookId: Long,
    val locator: String,
    val progression: Double,
    val position: Int?,
    val updatedAt: Instant,
)

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["bookId"])],
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val startedAt: Instant,
    val endedAt: Instant,
    val pagesRead: Int,
)

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["bookId"])],
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val locator: String,
    val text: String,
    val note: String?,
    val color: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** A bookmarked page (#37). Added in database version 2. */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["bookId"])],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val locator: String,
    val progression: Double,
    val text: String?,
    val createdAt: Instant,
)
