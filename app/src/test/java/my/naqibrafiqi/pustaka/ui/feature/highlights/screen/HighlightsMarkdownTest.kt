package my.naqibrafiqi.pustaka.ui.feature.highlights.screen

import my.naqibrafiqi.pustaka.data.model.Highlight
import my.naqibrafiqi.pustaka.data.model.HighlightColor
import my.naqibrafiqi.pustaka.testing.testResources
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class HighlightsMarkdownTest {

    @Test
    fun seriesAndBooksAreHeadingsAndHighlightsAreQuotes() {
        val groups = listOf(
            SeriesHighlights(
                seriesId = 1,
                name = "Brunhild",
                books = listOf(
                    BookHighlights(
                        bookId = 1,
                        title = "Brunhild, Vol. 01",
                        highlights = listOf(
                            item("The dragon's maiden.", page = 12),
                            item("First line\nSecond line\n\nNew paragraph", note = "  A note  "),
                        ),
                    ),
                ),
            ),
            SeriesHighlights(
                seriesId = null,
                name = null,
                books = listOf(BookHighlights(bookId = 2, title = "Alone", highlights = listOf(item("Quote", page = 1)))),
            ),
        )

        val markdown = HighlightsMarkdown(testResources).build(groups, LocalDate.of(2026, 11, 14))

        assertEquals(
            """
            # Highlights

            Exported from Pustaka on 14 November 2026.

            ## Brunhild

            ### Brunhild, Vol. 01

            > The dragon's maiden.

            *Page 12*

            > First line
            > Second line
            >
            > New paragraph

            A note

            ## Standalone books

            ### Alone

            > Quote

            *Page 1*
            """.trimIndent() + "\n",
            markdown,
        )
    }

    @Test
    fun fileNamesLeaveOutCharactersFilesCantHave() {
        val markdown = HighlightsMarkdown(testResources)
        assertEquals("Pustaka highlights.md", markdown.fileName(null))
        assertEquals("Brunhild, Vol. 01 highlights.md", markdown.fileName("Brunhild, Vol. 01"))
        assertEquals("Re Zero 1 highlights.md", markdown.fileName("Re: Zero / 1?"))
    }

    private fun item(text: String, page: Int? = null, note: String? = null) = HighlightItem(
        highlight = Highlight(
            bookId = 1,
            locator = "{}",
            text = text,
            note = note,
            color = HighlightColor.YELLOW,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        ),
        page = page,
        progression = 0.0,
    )
}
