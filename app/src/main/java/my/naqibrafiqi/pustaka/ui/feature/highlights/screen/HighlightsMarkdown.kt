package my.naqibrafiqi.pustaka.ui.feature.highlights.screen

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
object HighlightsMarkdown {

    fun build(groups: List<SeriesHighlights>, exportedOn: LocalDate): String = buildString {
        append("# Highlights\n\n")
        append("Exported from Pustaka on ${DATE.format(exportedOn)}.\n")
        for (group in groups) {
            append("\n## ${group.name ?: STANDALONE}\n")
            for (book in group.books) {
                append("\n### ${book.title}\n")
                for (item in book.highlights) {
                    append('\n')
                    append(quote(item.highlight.text))
                    item.highlight.note?.let { note -> append("\n\n").append(note.trim()) }
                    item.page?.let { page -> append("\n\n*Page $page*") }
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
        val base = bookTitle?.let { "$it highlights" } ?: "Pustaka highlights"
        val safe = base.replace(UNSAFE, "").replace(Regex("\\s+"), " ").trim().ifEmpty { "Pustaka highlights" }
        return "$safe.md"
    }

    /** Every line of the text starts with `> `, so a highlight over several paragraphs stays one quote. */
    private fun quote(text: String): String =
        text.trim().lines().joinToString("\n") { line -> if (line.isBlank()) ">" else "> ${line.trim()}" }

    const val STANDALONE = "Standalone books"
    private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
    private val UNSAFE = Regex("""[\\/:*?"<>|\u0000-\u001F]""")
}
