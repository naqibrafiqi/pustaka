package my.naqibrafiqi.pustaka.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.feature.series.screen.edit.EditDetailsScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.edit.EditField
import my.naqibrafiqi.pustaka.ui.feature.series.screen.edit.EditUiState
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Edit details: typing, resetting a field to its default, and leaving with unsaved changes. */
@RunWith(AndroidJUnit4::class)
class EditDetailsScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val changes = mutableListOf<Pair<String, String>>()
    private val resets = mutableListOf<String>()
    private var navigatedBack = false

    private fun show(state: EditUiState) = compose.setContent {
        PustakaTheme {
            EditDetailsScreen(
                title = "Edit series",
                state = state,
                onNavigateBack = { navigatedBack = true },
                onChange = { key, value -> changes += key to value },
                onReset = { resets += it },
                onUseDetailsFromFile = {},
                onSave = {},
            )
        }
    }

    private fun form(name: String, hasChanges: Boolean = false) = EditUiState.Ready(
        subtitle = "Brunhild",
        fields = listOf(EditField("name", R.string.edit_field_name, name, "Brunhild")),
        canUseFileDetails = true,
        hasChanges = hasChanges,
    )

    @Test
    fun anEditedFieldCanBeResetToItsDefault() {
        show(form("Brunhild the Dragonslayer", hasChanges = true))

        compose.onNodeWithText("Default: Brunhild").assertIsDisplayed()
        compose.onNodeWithContentDescription("Reset Name").performClick()
        compose.onNodeWithText("Brunhild the Dragonslayer").performTextReplacement("Brunhild!")

        assertEquals(listOf("name"), resets)
        assertEquals("name" to "Brunhild!", changes.last())
    }

    @Test
    fun leavingWithChangesAsksFirst() {
        show(form("Brunhild the Dragonslayer", hasChanges = true))

        compose.onNodeWithContentDescription("Navigate back").performClick()
        compose.onNodeWithText("Discard changes?").assertIsDisplayed()
        assertFalse(navigatedBack)

        compose.onNodeWithText("Discard").performClick()
        assertEquals(true, navigatedBack)
    }
}
