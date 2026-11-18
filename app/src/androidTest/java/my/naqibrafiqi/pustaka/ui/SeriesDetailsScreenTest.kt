package my.naqibrafiqi.pustaka.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDetailsScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDetailsUiState
import my.naqibrafiqi.pustaka.ui.model.BookItem
import my.naqibrafiqi.pustaka.ui.model.BookState
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asUiText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Series Details: the "Continue reading" button (#52, D32). */
@RunWith(AndroidJUnit4::class)
class SeriesDetailsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun series(continueBookId: Long?) = SeriesDetailsUiState.Ready(
        name = "Brunhild",
        author = "Yuiko Agarizaki",
        illustrator = null,
        synopsis = null,
        genres = emptyList(),
        headerCovers = listOf(null),
        volumeCount = 2,
        finishedCount = 1,
        percent = 50,
        folderPath = "/storage/emulated/0/Pustaka/Brunhild",
        totalSize = "2.0 MB",
        indexedCount = 2,
        volumes = List(2) { index ->
            BookItem(
                id = index + 1L,
                title = "Brunhild, Vol. 0${index + 1}",
                credits = null,
                coverPath = null,
                state = if (index == 0) BookState.FINISHED else BookState.READING,
                percent = if (index == 0) 100 else 40,
                volumeLabel = "Volume ${index + 1}".asUiText(),
            )
        },
        isGrid = true,
        continueBookId = continueBookId,
    )

    private fun show(state: SeriesDetailsUiState.Ready, onContinueReading: (Long) -> Unit = {}) {
        compose.setContent {
            PustakaTheme {
                SeriesDetailsScreen(
                    state = state,
                    onNavigateBack = {},
                    onToggleLayout = {},
                    onRetry = {},
                    onOpenBook = {},
                    onContinueReading = onContinueReading,
                    onEdit = {},
                    onDelete = {},
                    snackbarHostState = SnackbarHostState(),
                )
            }
        }
    }

    @Test
    fun continueReadingOpensTheCurrentVolume() {
        var opened: Long? = null
        show(series(continueBookId = 2), onContinueReading = { opened = it })

        // Read out by screen readers, which don't get the button's text (see the screen).
        compose.onNodeWithContentDescription("Continue reading").assertIsDisplayed().performClick()

        assertEquals(2L, opened)
    }

    @Test
    fun thereIsNoButtonWhenEveryVolumeIsFinished() {
        show(series(continueBookId = null))

        compose.onAllNodesWithText("Brunhild").onFirst().assertIsDisplayed()
        compose.onNodeWithContentDescription("Continue reading").assertDoesNotExist()
    }
}
