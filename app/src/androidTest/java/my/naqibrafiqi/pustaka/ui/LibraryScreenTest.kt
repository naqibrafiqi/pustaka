package my.naqibrafiqi.pustaka.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import my.naqibrafiqi.pustaka.ui.feature.library.screen.CurrentlyReading
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryFilter
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryScreen
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryUiState
import my.naqibrafiqi.pustaka.ui.model.BookItem
import my.naqibrafiqi.pustaka.ui.model.BookState
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asUiText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Library tab: the book being read, the filters and the empty library. */
@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val reading = BookItem(
        id = 7,
        title = "Brunhild, Vol. 02",
        credits = "Yuiko Agarizaki",
        coverPath = null,
        state = BookState.READING,
        percent = 42,
        volumeLabel = "Volume 2".asUiText(),
    )

    private val ready = LibraryUiState.Ready(
        currentlyReading = CurrentlyReading(reading, "Read 32m ago".asUiText(), "Page 66 of 157".asUiText()),
        filter = LibraryFilter.ALL,
        counts = mapOf(LibraryFilter.ALL to 3, LibraryFilter.READING to 1, LibraryFilter.UNREAD to 2),
        books = listOf(reading),
        isGrid = true,
        libraryFolderPath = "/storage/emulated/0/Pustaka",
    )

    private var resumed: Long? = null
    private var opened: Long? = null
    private var filter: LibraryFilter? = null
    private var searched = false
    private var toggledLayout = false

    private fun show(state: LibraryUiState) = compose.setContent {
        PustakaTheme {
            LibraryScreen(
                state = state,
                isRefreshing = false,
                onRefresh = {},
                onSelectFilter = { filter = it },
                onToggleLayout = { toggledLayout = true },
                onRetry = {},
                onOpenBook = { opened = it },
                onResumeBook = { resumed = it },
                onSearch = { searched = true },
            )
        }
    }

    @Test
    fun resumeOpensTheBookBeingRead() {
        show(ready)

        compose.onNode(hasText("Currently Reading") and isHeading()).assertIsDisplayed()
        compose.onNodeWithText("Read 32m ago").assertIsDisplayed()
        compose.onNodeWithText("Page 66 of 157").assertIsDisplayed()
        compose.onNodeWithText("Resume").performClick()

        assertEquals(7L, resumed)
    }

    @Test
    fun filtersShowTheirCounts() {
        show(ready)

        compose.onNodeWithText("All books (3)").assertIsDisplayed()
        compose.onNodeWithText("Currently reading (1)").performClick()

        assertEquals(LibraryFilter.READING, filter)
    }

    @Test
    fun topBarButtonsAreLabelled() {
        show(ready)

        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithContentDescription("Show as list").performClick()

        assertEquals(true, searched)
        assertEquals(true, toggledLayout)
    }

    @Test
    fun anEmptyLibrarySaysWhereToPutBooks() {
        show(
            ready.copy(
                currentlyReading = null,
                counts = LibraryFilter.entries.associateWith { 0 },
                books = emptyList(),
            )
        )

        compose.onNode(hasText("Your library is empty") and isHeading()).assertIsDisplayed()
        compose.onNodeWithText("/storage/emulated/0/Pustaka", substring = true).assertIsDisplayed()
    }
}
