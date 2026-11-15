package my.naqibrafiqi.pustaka.ui.model

import androidx.annotation.StringRes
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.model.authors
import my.naqibrafiqi.pustaka.data.model.illustrators
import my.naqibrafiqi.pustaka.util.text.UiText
import my.naqibrafiqi.pustaka.util.text.pluralText
import my.naqibrafiqi.pustaka.util.text.uiText

/** Where a book is, as shown on cards and in Book Details. A missing file wins over the rest. */
enum class BookState(@param:StringRes val label: Int) {
    UNREAD(R.string.book_state_unread),
    READING(R.string.book_state_reading),
    FINISHED(R.string.book_state_finished),
    MISSING(R.string.book_state_missing);

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
    val volumeLabel: UiText?,
) {
    val progress: Float get() = percent / 100f
    val isMissing: Boolean get() = state == BookState.MISSING
}

fun Book.toItem(progress: ReadingProgress?, volumeLabel: UiText? = null): BookItem {
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
fun pageLabel(position: Int?, totalPositions: Int?): UiText? = when {
    position != null && totalPositions != null -> uiText(R.string.page_of_total, position, totalPositions)
    position != null -> uiText(R.string.page_number, position)
    totalPositions != null -> pagesLabel(totalPositions)
    else -> null
}

fun pagesLabel(count: Int): UiText = pluralText(R.plurals.pages_count, count)

fun volumesLabel(count: Int): UiText = pluralText(R.plurals.volumes_count, count)

fun booksLabel(count: Int): UiText = pluralText(R.plurals.books_count, count)
