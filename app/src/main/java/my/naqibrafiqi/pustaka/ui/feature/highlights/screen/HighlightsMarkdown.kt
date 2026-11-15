package my.naqibrafiqi.pustaka.ui.feature.highlights.screen

import android.content.Context
import android.content.res.Resources
import dagger.hilt.android.qualifiers.ApplicationContext
import my.naqibrafiqi.pustaka.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Writes highlights as Markdown, for exporting (decisions D11 and D12). Series and books are
 * headings, each highlight is a quote, and its note follows it as a paragraph.
 *
 * ```
 * # Highlights
 *
 * Exported from Pustaka on 14 November 2026.
 *
 * ## Brunhild
 *
 * ### Brunhild, Vol. 01
 *
 * > The quote.
 *
 * A note.
 *
 * *Page 12*
 * ```
 */
class HighlightsMarkdown(private val resources: Resources) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context.resources)

    fun build(groups: List<SeriesHighlights>, exportedOn: LocalDate): String = buildString {
        append("# ${resources.getString(R.string.highlights_title)}\n\n")
        append(resources.getString(R.string.highlights_export_date, DATE.format(exportedOn))).append('\n')
        for (group in groups) {
            append("\n## ${group.name ?: resources.getString(R.string.highlights_standalone)}\n")
            for (book in group.books) {
                append("\n### ${book.title}\n")
                for (item in book.highlights) {
                    append('\n')
                    append(quote(item.highlight.text))
                    item.highlight.note?.let { note -> append("\n\n").append(note.trim()) }
                    item.page?.let { page -> append("\n\n*${resources.getString(R.string.page_number, page)}*") }
                    append('\n')
                }
            }
        }
    }

    /**
     * A file name for the export: the book's title for one book, or "Pustaka highlights" for
     * everything. Characters that file names can't have are left out.
     */
    fun fileName(bookTitle: String?): String {
        val everything = resources.getString(R.string.highlights_export_file_all)
        val base = bookTitle?.let { resources.getString(R.string.highlights_export_file_book, it) } ?: everything
        val safe = base.replace(UNSAFE, "").replace(Regex("\\s+"), " ").trim().ifEmpty { everything }
        return "$safe.md"
    }

    /** Every line of the text starts with `> `, so a highlight over several paragraphs stays one quote. */
    private fun quote(text: String): String =
        text.trim().lines().joinToString("\n") { line -> if (line.isBlank()) ">" else "> ${line.trim()}" }

    private companion object {
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
        val UNSAFE = Regex("""[\\/:*?"<>|\u0000-\u001F]""")
    }
}
