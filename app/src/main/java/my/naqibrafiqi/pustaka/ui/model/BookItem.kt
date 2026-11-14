package my.naqibrafiqi.pustaka.ui.model

import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.model.authors
import my.naqibrafiqi.pustaka.data.model.illustrators

/** Where a book is, as shown on cards and in Book Details. A missing file wins over the rest. */
enum class BookState(val label: String) {
    UNREAD("Unread"),
    READING("Reading"),
    FINISHED("Finished"),
    MISSING("File not found");

    companion object {
        fun of(book: Book): BookState = when {
            book.isMissing -> MISSING
            book.readingStatus == ReadingStatus.FINISHED -> FINISHED
            book.readingStatus == ReadingStatus.READING -> READING
            else -> UNREAD
        }
    }
}

/** A book as shown on a card. */
data class BookItem(
    val id: Long,
    val title: String,
    /** Authors and illustrators, for example "Yushi Ukai, Nekometaru". */
    val credits: String?,
    val coverPath: String?,
    val state: BookState,
    /** From 0 to 100. A finished book is always 100. */
    val percent: Int,
    /** "Volume 4", for a volume of a series. */
    val volumeLabel: String?,
) {
    val progress: Float get() = percent / 100f
    val isMissing: Boolean get() = state == BookState.MISSING
}

fun Book.toItem(progress: ReadingProgress?, volumeLabel: String? = null): BookItem {
    val state = BookState.of(this)
    return BookItem(
        id = id,
        title = name.value,
        credits = details.creditLine(),
        coverPath = details.coverPath,
        state = state,
        percent = percentRead(progress),
        volumeLabel = volumeLabel,
    )
}

/** Finished books count as 100%, and unread books as 0% even if an old position is left over. */
fun Book.percentRead(progress: ReadingProgress?): Int = when (readingStatus) {
    ReadingStatus.FINISHED -> 100
    ReadingStatus.UNREAD -> 0
    ReadingStatus.READING -> progress?.percent ?: 0
}

/** Authors first, then illustrators, without repeating a name. Null when the file names nobody. */
fun BookDetails.creditLine(): String? =
    (authors + illustrators).distinct().joinToString(", ").ifEmpty { null }

/** "Page 116 of 200", "Page 116" or "200 pages", or null when nothing is known. */
fun pageLabel(position: Int?, totalPositions: Int?): String? = when {
    position != null && totalPositions != null -> "Page $position of $totalPositions"
    position != null -> "Page $position"
    totalPositions != null -> pagesLabel(totalPositions)
    else -> null
}

fun pagesLabel(count: Int): String = if (count == 1) "1 page" else "$count pages"

fun volumesLabel(count: Int): String = if (count == 1) "1 volume" else "$count volumes"

fun booksLabel(count: Int): String = if (count == 1) "1 book" else "$count books"
