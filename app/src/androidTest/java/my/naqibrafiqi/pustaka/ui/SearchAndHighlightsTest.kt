package my.naqibrafiqi.pustaka.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import my.naqibrafiqi.pustaka.data.model.Bookmark
import my.naqibrafiqi.pustaka.data.model.Highlight
import my.naqibrafiqi.pustaka.data.model.HighlightColor
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.BookBookmarks
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.BookHighlights
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.BookmarkItem
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.HighlightItem
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.HighlightsScreen
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.HighlightsUiState
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.SeriesBookmarks
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.SeriesHighlights
import my.naqibrafiqi.pustaka.ui.feature.reader.ColorPicker
import my.naqibrafiqi.pustaka.ui.feature.search.screen.SearchResult
import my.naqibrafiqi.pustaka.ui.feature.search.screen.SearchResults
import my.naqibrafiqi.pustaka.ui.feature.search.screen.SearchScreen
import my.naqibrafiqi.pustaka.ui.feature.search.screen.SearchUiState
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asUiText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Searching the library, the Highlights tab, and picking a highlight's colour. */
@RunWith(AndroidJUnit4::class)
class SearchAndHighlightsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun searchResultsAreGroupedAndOpen() {
        val queries = mutableListOf<String>()
        var openedSeries: Long? = null
        var openedBook: Long? = null
        compose.setContent {
            var query by remember { mutableStateOf("") }
            PustakaTheme {
                SearchScreen(
                    query = query,
                    state = SearchUiState.Results(
                        query = "brun",
                        results = SearchResults(
                            series = listOf(SearchResult(1, "Brunhild", "Yuiko Agarizaki".asUiText(), null, null)),
                            books = listOf(SearchResult(2, "Brunhild, Vol. 01", "In Brunhild".asUiText(), null, null)),
                        ),
                    ),
                    onQueryChange = {
                        query = it
                        queries += it
                    },
                    onOpenSeries = { openedSeries = it },
                    onOpenBook = { openedBook = it },
                    onNavigateBack = {},
                )
            }
        }

        compose.onNodeWithText("Series, books, authors, genres").performTextInput("brun")
        compose.onNode(hasText("Series (1)") and isHeading()).assertIsDisplayed()
        compose.onNode(hasText("Books (1)") and isHeading()).assertIsDisplayed()
        compose.onNodeWithText("Brunhild").performClick()
        compose.onNodeWithText("Brunhild, Vol. 01").performClick()

        assertEquals("brun", queries.last())
        assertEquals(1L, openedSeries)
        assertEquals(2L, openedBook)
    }

    @Test
    fun highlightsAreGroupedByBookAndCanBeExported() {
        val highlight = Highlight(
            id = 5,
            bookId = 2,
            locator = "{}",
            text = "The dragon's maiden looked up at the sky.",
            note = "Foreshadowing?",
            color = HighlightColor.GREEN,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        val groups = listOf(
            SeriesHighlights(
                seriesId = 1,
                name = "Brunhild",
                books = listOf(BookHighlights(2, "Brunhild, Vol. 01", listOf(HighlightItem(highlight, page = 12, progression = 0.1)))),
            )
        )
        var opened: Highlight? = null
        var exported: Long? = null
        compose.setContent {
            PustakaTheme {
                HighlightsScreen(
                    state = HighlightsUiState.Ready(allGroups = groups, groups = groups, query = ""),
                    snackbarHostState = SnackbarHostState(),
                    onSearch = {},
                    onRetry = {},
                    onOpen = { opened = it },
                    onEdit = {},
                    onExportBook = { exported = it },
                    onExportAll = {},
                    onOpenBookmark = {},
                    onDeleteBookmark = {},
                )
            }
        }

        compose.onNode(hasText("Brunhild") and isHeading()).assertIsDisplayed()
        compose.onNodeWithText("Highlights (1)").assertIsSelected()
        compose.onAllNodesWithText("1 highlight").assertCountEquals(1)
        compose.onNodeWithText("Foreshadowing?").assertIsDisplayed()
        compose.onNodeWithText("The dragon's maiden looked up at the sky.").performClick()
        compose.onNodeWithContentDescription("Export highlights from Brunhild, Vol. 01 as Markdown").performClick()

        assertEquals(5L, opened?.id)
        assertEquals(2L, exported)
    }

    @Test
    fun bookmarksAreOnTheirOwnTab() {
        val bookmark = Bookmark(id = 7, bookId = 2, locator = "{}", progression = 0.42, createdAt = Instant.EPOCH)
        val bookmarks = listOf(
            SeriesBookmarks(
                seriesId = 1,
                name = "Brunhild",
                books = listOf(
                    BookBookmarks(
                        2,
                        "Brunhild, Vol. 01",
                        listOf(BookmarkItem(bookmark, chapter = "Chapter 3", page = 42, percent = 42, text = "The rain kept falling.")),
                    )
                ),
            )
        )
        var opened: Bookmark? = null
        var deleted: Long? = null
        compose.setContent {
            PustakaTheme {
                HighlightsScreen(
                    state = HighlightsUiState.Ready(
                        allGroups = emptyList(),
                        groups = emptyList(),
                        query = "",
                        allBookmarks = bookmarks,
                        bookmarks = bookmarks,
                    ),
                    snackbarHostState = SnackbarHostState(),
                    onSearch = {},
                    onRetry = {},
                    onOpen = {},
                    onEdit = {},
                    onExportBook = {},
                    onExportAll = {},
                    onOpenBookmark = { opened = it },
                    onDeleteBookmark = { deleted = it },
                )
            }
        }

        compose.onNodeWithText("No highlights yet").assertIsDisplayed()
        compose.onNodeWithText("Bookmarks (1)").performClick()

        compose.onNodeWithText("Bookmarks (1)").assertIsSelected()
        compose.onNodeWithText("1 bookmark").assertIsDisplayed()
        compose.onNodeWithText("Page 42 · 42%").assertIsDisplayed()
        compose.onNodeWithText("The rain kept falling.").assertIsDisplayed()
        compose.onNodeWithText("Chapter 3").performClick()
        compose.onNodeWithContentDescription("Remove bookmark").performClick()

        assertEquals(7L, opened?.id)
        assertEquals(7L, deleted)
    }

    @Test
    fun colourSwatchesAreNamedAndBigEnoughToTap() {
        var picked: HighlightColor? = null
        compose.setContent {
            PustakaTheme { ColorPicker(selected = HighlightColor.YELLOW, onSelect = { picked = it }) }
        }

        compose.onNodeWithContentDescription("Yellow").assertIsSelected()
        HighlightColor.entries.forEach { color ->
            compose.onNodeWithContentDescription(color.name.lowercase().replaceFirstChar(Char::uppercase))
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
        compose.onNodeWithContentDescription("Pink").performClick()

        assertEquals(HighlightColor.PINK, picked)
    }
}
