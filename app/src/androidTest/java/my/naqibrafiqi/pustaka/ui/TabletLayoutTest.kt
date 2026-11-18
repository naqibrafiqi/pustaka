package my.naqibrafiqi.pustaka.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import my.naqibrafiqi.pustaka.ui.components.BottomBarNavItems
import my.naqibrafiqi.pustaka.ui.components.SideRail
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryFilter
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryScreen
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryUiState
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDetailsScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDetailsUiState
import my.naqibrafiqi.pustaka.ui.model.BookItem
import my.naqibrafiqi.pustaka.ui.model.BookState
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asUiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screens drawn at a phone's size and at a tablet's (#39). The tablet sizes are the space next to
 * the navigation rail on an 800 × 1280dp tablet, upright and on its side.
 */
@RunWith(AndroidJUnit4::class)
class TabletLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private val phone = DpSize(412.dp, 915.dp)
    private val tabletUpright = DpSize(720.dp, 1280.dp)
    private val tabletSideways = DpSize(1200.dp, 800.dp)

    private val books = List(8) { index ->
        BookItem(
            id = index.toLong(),
            title = "Book ${index + 1}",
            credits = "Author",
            coverPath = null,
            state = BookState.UNREAD,
            percent = 0,
            volumeLabel = null,
        )
    }

    private fun showAt(size: DpSize, content: @Composable () -> Unit) = compose.setContent {
        DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
            PustakaTheme { content() }
        }
    }

    private fun showLibrary(size: DpSize, isGrid: Boolean = true) = showAt(size) {
        LibraryScreen(
            state = LibraryUiState.Ready(
                currentlyReading = null,
                filter = LibraryFilter.ALL,
                counts = LibraryFilter.entries.associateWith { books.size },
                books = books,
                isGrid = isGrid,
                libraryFolderPath = "/storage/emulated/0/Pustaka",
            ),
            isRefreshing = false,
            onRefresh = {},
            onSelectFilter = {},
            onToggleLayout = {},
            onRetry = {},
            onOpenBook = {},
            onResumeBook = {},
            onSearch = {},
        )
    }

    /**
     * How many books share the first book's row. Only the rows on screen are drawn. Pixels are
     * compared, because the forced size draws the screen at its own density.
     */
    private fun booksInFirstRow(): Int {
        val top = compose.onNodeWithText("Book 1").fetchSemanticsNode().boundsInRoot.top
        return compose.onAllNodes(hasText("Book ", substring = true))
            .fetchSemanticsNodes()
            .count { node -> node.boundsInRoot.top == top }
    }

    @Test
    fun railTabsAreCentredDownTheSide() {
        compose.setContent {
            PustakaTheme {
                Box(Modifier.size(width = 96.dp, height = 800.dp)) {
                    SideRail(currentItem = BottomBarNavItems[0], items = BottomBarNavItems, onClick = { _, _ -> })
                }
            }
        }
        val rail = compose.onRoot().getUnclippedBoundsInRoot()
        val top = compose.onNodeWithText("Library").getUnclippedBoundsInRoot().top
        val bottom = compose.onNodeWithText("Settings").getUnclippedBoundsInRoot().bottom
        val spaceAbove = top - rail.top
        val spaceBelow = rail.bottom - bottom
        assertTrue("The tabs start well below the top (#56)", spaceAbove > 200.dp)
        // The labels sit low in their tabs, so the space below is a little smaller.
        assertEquals(spaceAbove.value, spaceBelow.value, 40f)
    }

    @Test
    fun libraryGridIsTwoAcrossOnAPhone() {
        showLibrary(phone)
        assertEquals(2, booksInFirstRow())
    }

    @Test
    fun libraryGridFitsMoreCoversOnATablet() {
        showLibrary(tabletUpright)
        assertEquals(4, booksInFirstRow())
    }

    @Test
    fun libraryListIsOneAcrossOnAPhone() {
        showLibrary(phone, isGrid = false)
        assertEquals(1, booksInFirstRow())
    }

    @Test
    fun libraryListIsTwoAcrossOnATablet() {
        showLibrary(tabletUpright, isGrid = false)
        assertEquals(2, booksInFirstRow())
    }

    @Test
    fun bookDetailsShowsTheReadButtonWithoutScrollingOnATabletOnItsSide() {
        showAt(tabletSideways) {
            BookDetailsScreen(
                state = BookDetailsUiState.Ready(
                    title = "Brunhild, Vol. 01",
                    seriesLine = "Brunhild · Volume 1".asUiText(),
                    credits = "Yuiko Agarizaki",
                    coverPath = null,
                    genres = emptyList(),
                    synopsis = null,
                    state = BookState.READING,
                    percent = 42,
                    pageLabel = "Page 66 of 157".asUiText(),
                    continuePage = 66,
                    pageCount = "157 pages".asUiText(),
                    velocity = "Avg. 40 pages/h".asUiText(),
                    timeLeft = "~2h 15m".asUiText(),
                    info = emptyList(),
                ),
                onNavigateBack = {},
                onRetry = {},
                onEdit = {},
                onDelete = {},
                onRead = {},
                snackbarHostState = SnackbarHostState(),
            )
        }

        compose.onNodeWithText("Continue From Page 66").assertIsDisplayed()

        // The page is kept to a readable width in the middle, not stretched across the screen.
        val title = compose.onNodeWithText("Brunhild, Vol. 01").getUnclippedBoundsInRoot()
        assertTrue("title starts at ${title.left}", title.left > 200.dp)
    }
}
