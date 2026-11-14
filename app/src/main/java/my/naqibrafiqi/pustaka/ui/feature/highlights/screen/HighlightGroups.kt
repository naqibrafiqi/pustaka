package my.naqibrafiqi.pustaka.ui.feature.highlights.screen

import my.naqibrafiqi.pustaka.data.library.VolumeOrder
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.Highlight
import my.naqibrafiqi.pustaka.data.model.Series
import org.json.JSONObject

/**
 * A highlight in the Highlights tab.
 *
 * @property page the page it starts on, or null when the book has no page numbers.
 * @property progression how far through the book it is, from 0 to 1, for putting a book's
 *   highlights in reading order.
 */
data class HighlightItem(
    val highlight: Highlight,
    val page: Int?,
    val progression: Double,
) {
    val id: Long get() = highlight.id
}

/** A book's highlights, in the order they come in the book. */
data class BookHighlights(
    val bookId: Long,
    val title: String,
    val highlights: List<HighlightItem>,
)

/**
 * The highlights from one series, or from the standalone books when [seriesId] is null.
 *
 * @property name the series' name, or null for standalone books.
 */
data class SeriesHighlights(
    val seriesId: Long?,
    val name: String?,
    val books: List<BookHighlights>,
) {
    val highlightCount: Int get() = books.sumOf { it.highlights.size }
}

/**
 * Groups highlights by series and book: series by name, their volumes in reading order (see
 * [VolumeOrder]), then standalone books by name. Each book's highlights are in the order they come
 * in the book.
 *
 * Highlights in books that aren't in [books], such as books removed from the library, are left out.
 *
 * @param books the books in the library.
 * @param series the series in the library.
 */
fun groupHighlights(highlights: List<Highlight>, books: List<Book>, series: List<Series>): List<SeriesHighlights> {
    val byBook = highlights.groupBy { it.bookId }
    val booksWithHighlights = books.filter { it.id in byBook }

    fun bookGroup(book: Book) = BookHighlights(
        bookId = book.id,
        title = book.name.value,
        highlights = byBook.getValue(book.id)
            .map(::itemOf)
            .sortedWith(compareBy<HighlightItem> { it.progression }.thenBy { it.highlight.createdAt }.thenBy { it.id }),
    )

    val volumesBySeries = booksWithHighlights.filterNot { it.isStandalone }.groupBy { it.seriesId }
    val seriesGroups = series
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.value })
        .mapNotNull { s ->
            val volumes = volumesBySeries[s.id] ?: return@mapNotNull null
            SeriesHighlights(seriesId = s.id, name = s.name.value, books = VolumeOrder.sort(volumes).map(::bookGroup))
        }

    val standalone = booksWithHighlights
        .filter { it.isStandalone }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.value })
        .map(::bookGroup)
    val standaloneGroup = standalone.takeIf { it.isNotEmpty() }?.let { SeriesHighlights(seriesId = null, name = null, books = it) }

    return seriesGroups + listOfNotNull(standaloneGroup)
}

/**
 * Keeps the highlights whose text or note contains [query], ignoring case. A highlight also matches
 * when its book's title or its series' name does, so a search for a book shows all of its
 * highlights. A blank query keeps everything.
 */
fun List<SeriesHighlights>.search(query: String): List<SeriesHighlights> {
    val words = query.trim()
    if (words.isEmpty()) return this
    fun String?.matches() = this?.contains(words, ignoreCase = true) == true

    return mapNotNull { group ->
        val books = group.books.mapNotNull { book ->
            val highlights = if (group.name.matches() || book.title.matches()) {
                book.highlights
            } else {
                book.highlights.filter { it.highlight.text.matches() || it.highlight.note.matches() }
            }
            highlights.takeIf { it.isNotEmpty() }?.let { book.copy(highlights = it) }
        }
        books.takeIf { it.isNotEmpty() }?.let { group.copy(books = it) }
    }
}

/** Reads the page and how far through the book a highlight is from its saved Readium locator. */
private fun itemOf(highlight: Highlight): HighlightItem {
    val locations = runCatching { JSONObject(highlight.locator).optJSONObject("locations") }.getOrNull()
    val page = locations?.optInt("position", 0)?.takeIf { it > 0 }
    val progression = locations?.optDouble("totalProgression", Double.NaN)?.takeUnless { it.isNaN() }
    return HighlightItem(highlight = highlight, page = page, progression = progression ?: 0.0)
}
