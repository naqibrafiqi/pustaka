package my.naqibrafiqi.pustaka.ui.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner

/** Readium's links are built on Android's Uri, so these run under Robolectric. */
@RunWith(RobolectricTestRunner::class)
class ReaderNavigationTest {

    @Test
    fun edgesTurnThePageAndTheMiddleShowsTheMenus() {
        assertEquals(TapZone.PREVIOUS, TapZone.of(x = 50f, width = 1000))
        assertEquals(TapZone.MENU, TapZone.of(x = 500f, width = 1000))
        assertEquals(TapZone.NEXT, TapZone.of(x = 950f, width = 1000))
        // Right on the line counts as the middle.
        assertEquals(TapZone.MENU, TapZone.of(x = 300f, width = 1000))
        assertEquals(TapZone.MENU, TapZone.of(x = 700f, width = 1000))
    }

    @Test
    fun rightToLeftBooksGoForwardFromTheLeftEdge() {
        assertEquals(TapZone.NEXT, TapZone.of(x = 50f, width = 1000, rightToLeft = true))
        assertEquals(TapZone.PREVIOUS, TapZone.of(x = 950f, width = 1000, rightToLeft = true))
    }

    @Test
    fun aPageWithNoWidthOnlyShowsTheMenus() {
        assertEquals(TapZone.MENU, TapZone.of(x = 0f, width = 0))
    }

    @Test
    fun flattensNestedChaptersWithTheirDepth() {
        val toc = listOf(
            link("cover.xhtml", "Cover"),
            link(
                "part1.xhtml", "Part 1",
                children = listOf(
                    link("ch1.xhtml", "Chapter 1"),
                    link("ch1.xhtml#scene2", "Scene 2", children = listOf(link("ch1.xhtml#deep", "Deep"))),
                ),
            ),
            link("afterword.xhtml", "Afterword"),
        )

        val entries = flattenTableOfContents(toc)

        assertEquals(
            listOf("Cover" to 0, "Part 1" to 0, "Chapter 1" to 1, "Scene 2" to 1, "Deep" to 2, "Afterword" to 0),
            entries.map { it.title to it.depth },
        )
    }

    @Test
    fun untitledEntriesAreSkippedAndTheirChildrenMoveUp() {
        val toc = listOf(
            link("part1.xhtml", title = "  ", children = listOf(link("ch1.xhtml", "Chapter 1"))),
        )

        assertEquals(listOf("Chapter 1" to 0), flattenTableOfContents(toc).map { it.title to it.depth })
    }

    @Test
    fun currentChapterIsTheLastEntryInTheFile() {
        val entries = flattenTableOfContents(
            listOf(
                link("ch1.xhtml", "Chapter 1"),
                link("ch1.xhtml#scene2", "Scene 2"),
                link("ch2.xhtml", "Chapter 2"),
            ),
        )

        assertEquals(1, currentTocIndex(entries, "ch1.xhtml"))
        assertEquals(2, currentTocIndex(entries, "ch2.xhtml#top"))
    }

    @Test
    fun pagesBetweenChaptersBelongToTheChapterBefore() {
        val entries = flattenTableOfContents(
            listOf(link("ch1.xhtml", "Chapter 1"), link("ch2.xhtml", "Chapter 2")),
        )
        val readingOrder = listOf("cover.xhtml", "ch1.xhtml", "insert.xhtml", "ch2.xhtml")

        assertEquals(0, currentTocIndex(entries, "insert.xhtml", readingOrder))
        // Nothing comes before the first chapter.
        assertNull(currentTocIndex(entries, "cover.xhtml", readingOrder))
        // A file that isn't in the book at all.
        assertNull(currentTocIndex(entries, "elsewhere.xhtml", readingOrder))
    }

    private fun link(href: String, title: String?, children: List<Link> = emptyList()) =
        Link(href = checkNotNull(Url(href)), title = title, children = children)
}
