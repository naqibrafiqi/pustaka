package my.naqibrafiqi.pustaka.ui.feature.search.screen

import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.ui.model.creditLine
import java.text.Normalizer

/** A series or book that matches the search. */
data class SearchResult(
    val id: Long,
    val title: String,
    /** The credits, or the series a volume belongs to. */
    val subtitle: String?,
    /** Why it matched when it isn't the title, for example "Genre: Fantasy". */
    val matchedOn: String?,
    val coverPath: String?,
)

data class SearchResults(val series: List<SearchResult>, val books: List<SearchResult>) {
    val isEmpty: Boolean get() = series.isEmpty() && books.isEmpty()

    companion object {
        val NONE = SearchResults(emptyList(), emptyList())
    }
}

/**
 * Finds series and books by name, author and genre (decision D17). Matching ignores case and
 * accents, so "kotoba" finds "Kotobá". Every word of the query has to match somewhere in the same
 * series or book, so "brunhild agarizaki" finds the series by its name and author together.
 */
object LibrarySearch {

    fun search(query: String, series: List<Series>, books: List<Book>): SearchResults {
        val words = normalize(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return SearchResults.NONE
        val seriesNames = series.associate { it.id to it.name.value }
        return SearchResults(
            series = series.mapNotNull { match(words, it.searchFields())?.let { matched -> it.toResult(matched) } },
            books = books.mapNotNull { book ->
                match(words, book.searchFields())?.let { matched -> book.toResult(matched, seriesNames[book.seriesId]) }
            },
        )
    }

    /**
     * Null when some word isn't found. Otherwise the first field that isn't the title and was
     * needed for a match, or an empty string when the title alone matched.
     */
    private fun match(words: List<String>, fields: List<Field>): String? {
        val normalized = fields.map { it to normalize(it.value) }
        var reason = ""
        for (word in words) {
            val hit = normalized.firstOrNull { (_, text) -> word in text }?.first ?: return null
            if (!hit.isTitle && reason.isEmpty()) reason = "${hit.label}: ${hit.value}"
        }
        return reason
    }

    private class Field(val label: String, val value: String, val isTitle: Boolean = false)

    private fun Series.searchFields(): List<Field> = buildList {
        add(Field("Name", name.value, isTitle = true))
        details.author?.let { add(Field("Author", it)) }
        details.illustrator?.let { add(Field("Illustrator", it)) }
        details.mainGenre?.let { add(Field("Genre", it)) }
        details.tags.orEmpty().forEach { add(Field("Tag", it)) }
    }

    private fun Book.searchFields(): List<Field> = buildList {
        add(Field("Name", name.value, isTitle = true))
        details.title?.takeIf { it != name.value }?.let { add(Field("Title", it)) }
        details.creators.orEmpty().forEach { add(Field(if (it.isAuthor) "Author" else "By", it.name)) }
        details.subjects.orEmpty().forEach { add(Field("Genre", it)) }
    }

    private fun Series.toResult(matchedOn: String): SearchResult {
        val subtitle = listOfNotNull(details.author, details.illustrator).joinToString(", ").ifEmpty { null }
        return SearchResult(id, name.value, subtitle, reasonToShow(matchedOn, subtitle), details.coverPath)
    }

    private fun Book.toResult(matchedOn: String, seriesName: String?): SearchResult {
        val subtitle = seriesName?.let { "In $it" } ?: details.creditLine()
        return SearchResult(id, name.value, subtitle, reasonToShow(matchedOn, subtitle), details.coverPath)
    }

    /** The reason, unless the subtitle already shows the value that matched. */
    private fun reasonToShow(matchedOn: String, subtitle: String?): String? =
        matchedOn.takeIf { it.isNotEmpty() && subtitle?.contains(it.substringAfter(": ")) != true }

    /** Lower case, without accents, and with runs of spaces made single. */
    internal fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(ACCENTS, "")
            .lowercase()
            .replace(SPACES, " ")
            .trim()

    private val ACCENTS = Regex("\\p{Mn}+")
    private val SPACES = Regex("\\s+")
}
