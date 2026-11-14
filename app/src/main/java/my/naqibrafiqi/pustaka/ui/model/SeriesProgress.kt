package my.naqibrafiqi.pustaka.ui.model

import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import java.math.BigDecimal

/**
 * How far the reader is through a series. [volumes] must be in reading order (see
 * [my.naqibrafiqi.pustaka.data.library.VolumeOrder]).
 */
class SeriesProgress(private val volumes: List<Book>) {

    val total: Int get() = volumes.size

    val finished: Int get() = volumes.count { it.readingStatus == ReadingStatus.FINISHED }

    /** Finished volumes out of all volumes, from 0 to 100. */
    val percent: Int get() = if (total == 0) 0 else finished * 100 / total

    /**
     * The volume to carry on with: the one being read that was opened last, otherwise the first
     * volume that isn't finished. Volumes whose file is missing can't be read, so they're skipped.
     * Null when there's nothing left to read.
     */
    val current: Book? by lazy {
        val readable = volumes.filterNot { it.isMissing }
        readable.filter { it.readingStatus == ReadingStatus.READING }
            .maxWithOrNull(compareBy(nullsFirst()) { it.lastOpenedAt })
            ?: readable.firstOrNull { it.readingStatus != ReadingStatus.FINISHED }
    }

    /** "Volume 4" for [book], using its number, or its place in the series when it has none. */
    fun labelOf(book: Book): String {
        val number = book.volumeNumber
        return if (number != null) "Volume ${formatVolumeNumber(number)}" else "Volume ${volumes.indexOf(book) + 1}"
    }

    /** "Reading: Volume 2", "Up next: Volume 3", "All volumes read" or "No volumes yet". */
    val currentLabel: String
        get() {
            val book = current
            return when {
                total == 0 -> "No volumes yet"
                book == null && finished == total -> "All volumes read"
                book == null -> "No readable volumes"
                book.readingStatus == ReadingStatus.READING -> "Reading: ${labelOf(book)}"
                else -> "Up next: ${labelOf(book)}"
            }
        }

    /** Up to [size] volumes around the current one, so the preview shows where the reader is. */
    fun preview(size: Int = 3): List<Book> {
        if (volumes.size <= size) return volumes
        val index = current?.let { volumes.indexOf(it) } ?: (volumes.size - 1)
        val start = (index - 1).coerceIn(0, volumes.size - size)
        return volumes.subList(start, start + size)
    }
}

/** 4.0 is shown as "4" and 4.5 as "4.5". */
fun formatVolumeNumber(number: Double): String =
    BigDecimal.valueOf(number).stripTrailingZeros().toPlainString()
