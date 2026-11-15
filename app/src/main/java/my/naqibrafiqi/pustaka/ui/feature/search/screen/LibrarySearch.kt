package my.naqibrafiqi.pustaka.ui.feature.search.screen

import androidx.annotation.StringRes
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.ui.model.creditLine
import my.naqibrafiqi.pustaka.util.text.UiText
import my.naqibrafiqi.pustaka.util.text.asUiText
import my.naqibrafiqi.pustaka.util.text.uiText
import java.text.Normalizer

/** A series or book that matches the search. */
data class SearchResult(
    val id: Long,
    val title: String,
    /** The credits, or the series a volume belongs to. */
    val subtitle: UiText?,
    /** Why it matched when it isn't the title, for example "Genre: Fantasy". */
    val matchedOn: UiText?,
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
     * Null when some word isn't found. Otherwise a match whose [Match.reason] is the first field
     * that isn't the title and was needed, or null when the title alone matched.
     */
    private fun match(words: List<String>, fields: List<Field>): Match? {
        val normalized = fields.map { it to normalize(it.value) }
        var reason: Field? = null
        for (word in words) {
            val hit = normalized.firstOrNull { (_, text) -> word in text }?.first ?: return null
            if (!hit.isTitle && reason == null) reason = hit
        }
        return Match(reason)
    }

    private class Match(val reason: Field?)

    private class Field(@param:StringRes val label: Int, val value: String, val isTitle: Boolean = false)

    private fun Series.searchFields(): List<Field> = buildList {
        add(Field(R.string.edit_field_name, name.value, isTitle = true))
        details.author?.let { add(Field(R.string.details_author, it)) }
        details.illustrator?.let { add(Field(R.string.details_illustrator, it)) }
        details.mainGenre?.let { add(Field(R.string.search_field_genre, it)) }
        details.tags.orEmpty().forEach { add(Field(R.string.search_field_tag, it)) }
    }

    private fun Book.searchFields(): List<Field> = buildList {
        add(Field(R.string.edit_field_name, name.value, isTitle = true))
        details.title?.takeIf { it != name.value }?.let { add(Field(R.string.search_field_title, it)) }
        details.creators.orEmpty().forEach { add(Field(if (it.isAuthor) R.string.details_author else R.string.search_field_by, it.name)) }
        details.subjects.orEmpty().forEach { add(Field(R.string.search_field_genre, it)) }
    }

    private fun Series.toResult(match: Match): SearchResult {
        val credits = listOfNotNull(details.author, details.illustrator).joinToString(", ").ifEmpty { null }
        return SearchResult(id, name.value, credits?.asUiText(), reasonToShow(match, credits), details.coverPath)
    }

    private fun Book.toResult(match: Match, seriesName: String?): SearchResult {
        val subtitle = seriesName?.let { uiText(R.string.search_in_series, it) } ?: details.creditLine()?.asUiText()
        return SearchResult(id, name.value, subtitle, reasonToShow(match, seriesName ?: details.creditLine()), details.coverPath)
    }

    /** Why it matched, unless [shown] (what the subtitle shows) already has the value that matched. */
    private fun reasonToShow(match: Match, shown: String?): UiText? {
        val field = match.reason ?: return null
        if (shown?.contains(field.value) == true) return null
        return uiText(R.string.search_matched_on, uiText(field.label), field.value)
    }

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
