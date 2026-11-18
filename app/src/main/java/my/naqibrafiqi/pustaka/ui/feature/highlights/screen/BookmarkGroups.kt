package my.naqibrafiqi.pustaka.ui.feature.highlights.screen

import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.Bookmark
import my.naqibrafiqi.pustaka.data.model.Series
import org.json.JSONObject
import kotlin.math.floor

/**
 * A bookmark in the Highlights tab's Bookmarks list (#51, D33).
 *
 * @property chapter the chapter's name, as the reader saved it with the page, or null when the
 *   page isn't in the table of contents.
 * @property page the bookmarked page, or null when the book has no page numbers.
 * @property percent how far through the book, from 0 to 100, rounded down like everywhere else.
 * @property text the start of the page's text.
 */
data class BookmarkItem(
    val bookmark: Bookmark,
    val chapter: String?,
    val page: Int?,
    val percent: Int,
    val text: String?,
) {
    val id: Long get() = bookmark.id
}

/** A book's bookmarks, in book order. */
data class BookBookmarks(
    val bookId: Long,
    val title: String,
    val bookmarks: List<BookmarkItem>,
)

/**
 * The bookmarks in one series, or in the standalone books when [seriesId] is null.
 *
 * @property name the series' name, or null for standalone books.
 */
data class SeriesBookmarks(
    val seriesId: Long?,
    val name: String?,
    val books: List<BookBookmarks>,
) {
    val bookmarkCount: Int get() = books.sumOf { it.bookmarks.size }
}

/**
 * Groups bookmarks by series and book, in the same order as highlights (see [groupHighlights]).
 * Each book's bookmarks are in book order. Bookmarks in books that aren't in [books] are left out.
 */
fun groupBookmarks(bookmarks: List<Bookmark>, books: List<Book>, series: List<Series>): List<SeriesBookmarks> {
    val byBook = bookmarks.groupBy { it.bookId }
    return shelvesOf(byBook.keys, books, series).map { shelf ->
        SeriesBookmarks(
            seriesId = shelf.seriesId,
            name = shelf.name,
            books = shelf.books.map { book ->
                BookBookmarks(
                    bookId = book.id,
                    title = book.name.value,
                    bookmarks = byBook.getValue(book.id)
                        .sortedWith(compareBy<Bookmark> { it.progression }.thenBy { it.createdAt }.thenBy { it.id })
                        .map(::bookmarkItemOf),
                )
            },
        )
    }
}

/**
 * Keeps the bookmarks whose chapter or text contains [query], ignoring case. A bookmark also
 * matches when its book's title or its series' name does. A blank query keeps everything.
 */
fun List<SeriesBookmarks>.searchBookmarks(query: String): List<SeriesBookmarks> {
    val words = query.trim()
    if (words.isEmpty()) return this
    fun String?.matches() = this?.contains(words, ignoreCase = true) == true

    return mapNotNull { group ->
        val books = group.books.mapNotNull { book ->
            val bookmarks = if (group.name.matches() || book.title.matches()) {
                book.bookmarks
            } else {
                book.bookmarks.filter { it.chapter.matches() || it.text.matches() }
            }
            bookmarks.takeIf { it.isNotEmpty() }?.let { book.copy(bookmarks = it) }
        }
        books.takeIf { it.isNotEmpty() }?.let { group.copy(books = it) }
    }
}

/** Reads the chapter and page from the bookmark's saved Readium locator. */
private fun bookmarkItemOf(bookmark: Bookmark): BookmarkItem {
    val locator = runCatching { JSONObject(bookmark.locator) }.getOrNull()
    val chapter = locator?.takeUnless { it.isNull("title") }?.optString("title")?.trim()?.ifEmpty { null }
    val page = locator?.optJSONObject("locations")?.optInt("position", 0)?.takeIf { it > 0 }
    val progression = bookmark.progression.coerceIn(0.0, 1.0)
    return BookmarkItem(
        bookmark = bookmark,
        chapter = chapter,
        page = page,
        percent = floor(progression * 100 + 1e-9).toInt(),
        text = bookmark.text?.replace(WHITESPACE, " ")?.trim()?.ifEmpty { null },
    )
}

private val WHITESPACE = Regex("\\s+")
