package my.naqibrafiqi.pustaka.ui.feature.search

import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.Creator
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import my.naqibrafiqi.pustaka.testing.T0
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.ui.feature.search.screen.LibrarySearch
import my.naqibrafiqi.pustaka.ui.feature.search.screen.SearchResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchTest {

    private val brunhild = Series(
        id = 1,
        folderPath = "/lib/Brunhild",
        folderName = "Brunhild",
        editedName = "Brunhild the Dragonslayer",
        sourceDetails = SeriesDetails(author = "Yuiko Agarizaki", mainGenre = "Fantasy", tags = listOf("Tragedy")),
        createdAt = T0,
        updatedAt = T0,
    )
    private val divas = Series(
        id = 2,
        folderPath = "/lib/Battle Divas",
        folderName = "Battle Divas",
        sourceDetails = SeriesDetails(author = "Kouka Kishine", mainGenre = "Action"),
        createdAt = T0,
        updatedAt = T0,
    )
    private val volume = book("Brunhild, Vol. 01.epub", seriesId = 1, id = 10).copy(
        sourceDetails = BookDetails(creators = listOf(Creator("Yuiko Agarizaki", "aut")), subjects = listOf("Fantasy")),
    )
    private val standalone = book("Kotobá.epub", id = 11).copy(
        sourceDetails = BookDetails(creators = listOf(Creator("Nekonabeao", "ill"))),
    )

    private fun search(query: String) = LibrarySearch.search(query, listOf(brunhild, divas), listOf(volume, standalone))

    @Test
    fun findsSeriesAndBooksByName() {
        val results = search("brunhild")

        assertEquals(listOf("Brunhild the Dragonslayer"), results.series.map { it.title })
        assertNull(results.series.single().matchedOn)
        assertEquals(listOf("Brunhild, Vol. 01"), results.books.map { it.title })
        assertEquals("In Brunhild the Dragonslayer", results.books.single().subtitle)
    }

    @Test
    fun findsByAuthorAndGenreAndSaysWhy() {
        // The author is already the subtitle, so it isn't repeated.
        val byAuthor = search("kishine")
        assertEquals(listOf("Battle Divas"), byAuthor.series.map { it.title })
        assertEquals("Kouka Kishine", byAuthor.series.single().subtitle)
        assertNull(byAuthor.series.single().matchedOn)
        assertEquals("Author: Yuiko Agarizaki", search("agarizaki").books.single().matchedOn)

        val byGenre = search("FANTASY")
        assertEquals("Genre: Fantasy", byGenre.series.single().matchedOn)
        assertEquals("Genre: Fantasy", byGenre.books.single().matchedOn)

        assertEquals("Tag: Tragedy", search("tragedy").series.single().matchedOn)
        val byIllustrator = search("nekonabeao").books.single()
        assertTrue(byIllustrator.subtitle!!.contains("Nekonabeao"))
        assertNull(byIllustrator.matchedOn)
    }

    @Test
    fun everyWordHasToMatchTheSameItem() {
        assertEquals(listOf("Brunhild the Dragonslayer"), search("dragonslayer agarizaki").series.map { it.title })
        assertEquals(SearchResults.NONE, search("dragonslayer kishine"))
    }

    @Test
    fun ignoresAccentsCaseAndSpaces() {
        assertEquals(listOf("Kotobá"), search("  KOTOBA ").books.map { it.title })
        assertEquals(listOf("Kotobá"), search("kotobá").books.map { it.title })
    }

    @Test
    fun aBlankQueryFindsNothing() {
        assertEquals(SearchResults.NONE, search("   "))
    }
}
