package my.naqibrafiqi.pustaka.data.model

import my.naqibrafiqi.pustaka.data.library.VolumeOrder
import java.time.Duration
import java.time.Instant
import kotlin.math.floor

enum class ReadingStatus {
    UNREAD,
    READING,
    FINISHED,
}

/** A folder directly inside the library folder. Its EPUB files are the volumes. */
data class Series(
    val id: Long = 0,
    val folderPath: String,
    val folderName: String,
    val editedName: String? = null,
    val sourceDetails: SeriesDetails = SeriesDetails.EMPTY,
    val editedDetails: SeriesDetails = SeriesDetails.EMPTY,
    val isHidden: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    /** The folder name by default, or the user's own name for the series. */
    val name: Editable<String> get() = Editable(folderName, editedName)

    /** The details to show, with the user's edits applied. */
    val details: SeriesDetails get() = editedDetails.overlaidOn(sourceDetails)
}

/**
 * An EPUB file in the library: a volume of a series, or a standalone book when [seriesId] is null.
 *
 * @property fingerprint identifies the file's contents, so a renamed or moved file is recognised.
 * @property totalPositions the number of Readium positions, shown as "Page X of Y".
 */
data class Book(
    val id: Long = 0,
    val seriesId: Long? = null,
    val filePath: String,
    val fileName: String,
    val fingerprint: String,
    val fileSize: Long,
    val fileLastModified: Instant,
    val isMissing: Boolean = false,
    val isHidden: Boolean = false,
    val editedName: String? = null,
    val sourceDetails: BookDetails = BookDetails.EMPTY,
    val editedDetails: BookDetails = BookDetails.EMPTY,
    val totalPositions: Int? = null,
    val readingStatus: ReadingStatus = ReadingStatus.UNREAD,
    val addedAt: Instant,
    val lastOpenedAt: Instant? = null,
) {
    /** The filename without `.epub` by default, or the user's own name for the book. */
    val name: Editable<String> get() = Editable(defaultNameFor(fileName), editedName)

    /** The details to show, with the user's edits applied. */
    val details: BookDetails get() = editedDetails.overlaidOn(sourceDetails)

    /** The number used to order this volume in its series. See [VolumeOrder]. */
    val volumeNumber: Double? get() = VolumeOrder.volumeNumberOf(this)

    val isStandalone: Boolean get() = seriesId == null

    companion object {
        private const val EPUB_EXTENSION = ".epub"

        fun defaultNameFor(fileName: String): String =
            if (fileName.endsWith(EPUB_EXTENSION, ignoreCase = true)) {
                fileName.dropLast(EPUB_EXTENSION.length)
            } else {
                fileName
            }
    }
}

/**
 * Where the reader is in a book.
 *
 * @property locator the position as a serialized Readium `Locator` (JSON).
 * @property progression how far through the whole book, from 0.0 to 1.0.
 * @property position the current page, counted in Readium positions, starting at 1.
 */
data class ReadingProgress(
    val bookId: Long,
    val locator: String,
    val progression: Double,
    val position: Int?,
    val updatedAt: Instant,
) {
    /** Rounded down, so a book only shows 100% once it's actually at the end. */
    val percent: Int get() = floor(progression.coerceIn(0.0, 1.0) * 100 + PERCENT_EPSILON).toInt()

    private companion object {
        /** Absorbs floating-point error, e.g. 0.58 * 100 = 57.99999999999999. */
        const val PERCENT_EPSILON = 1e-9
    }
}

/** One stretch of reading, used for "Read 32m ago", "Velocity" and "Estimated Left". */
data class ReadingSession(
    val id: Long = 0,
    val bookId: Long,
    val startedAt: Instant,
    val endedAt: Instant,
    val pagesRead: Int,
) {
    val duration: Duration get() = Duration.between(startedAt, endedAt)
}

/**
 * A highlighted passage in a book, made by selecting text in the reader.
 *
 * @property locator the passage as a serialized Readium `Locator` (JSON), including the text
 *   around it, which the reader uses to find it again.
 */
data class Highlight(
    val id: Long = 0,
    val bookId: Long,
    val locator: String,
    val text: String,
    val note: String? = null,
    val color: HighlightColor,
    val createdAt: Instant,
    val updatedAt: Instant,
)
