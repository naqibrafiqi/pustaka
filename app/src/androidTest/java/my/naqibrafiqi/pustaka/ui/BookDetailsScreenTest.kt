package my.naqibrafiqi.pustaka.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDetailsScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDetailsUiState
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.InfoRow
import my.naqibrafiqi.pustaka.ui.model.BookState
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asUiText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Book Details: reading from the saved page or from the start, and "More info". */
@RunWith(AndroidJUnit4::class)
class BookDetailsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val started = BookDetailsUiState.Ready(
        title = "Brunhild, Vol. 01",
        seriesLine = "Brunhild · Volume 1".asUiText(),
        credits = "Yuiko Agarizaki",
        coverPath = null,
        genres = listOf("Fantasy"),
        synopsis = null,
        state = BookState.READING,
        percent = 42,
        pageLabel = "Page 66 of 157".asUiText(),
        continuePage = 66,
        pageCount = "157 pages".asUiText(),
        velocity = "Avg. 40 pages/h".asUiText(),
        timeLeft = "~2h 15m".asUiText(),
        info = listOf(InfoRow(R.string.book_info_publisher, "J-Novel Club".asUiText())),
    )

    private val reads = mutableListOf<Boolean>()

    private fun show(state: BookDetailsUiState) = compose.setContent {
        PustakaTheme {
            BookDetailsScreen(
                state = state,
                onNavigateBack = {},
                onRetry = {},
                onEdit = {},
                onDelete = {},
                onRead = { fromStart -> reads += fromStart },
                snackbarHostState = SnackbarHostState(),
            )
        }
    }

    @Test
    fun continuesFromTheSavedPageOrStartsAgain() {
        show(started)

        compose.onNodeWithText("Continue From Page 66").performScrollTo().performClick()
        compose.onNodeWithText("Read From Beginning").performScrollTo().performClick()

        assertEquals(listOf(false, true), reads)
    }

    @Test
    fun aMissingFileCantBeRead() {
        show(started.copy(state = BookState.MISSING))

        compose.onNodeWithText("File not found").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Continue From Page 66").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun moreInfoShowsTheDetailsFromTheFile() {
        show(started)

        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("More info").performClick()

        compose.onNodeWithText("Publisher").assertIsDisplayed()
        compose.onNodeWithText("J-Novel Club").assertIsDisplayed()
    }
}
