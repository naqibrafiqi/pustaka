package my.naqibrafiqi.pustaka

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * Opens the real app and moves between its tabs and the search screen. It only reads the library,
 * so it can run on a phone with books on it without changing anything.
 */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    /** Gives the app "All files access" before it starts, so it opens on the library. */
    private val allFilesAccess = TestRule { base: Statement, _: Description ->
        object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val packageName = instrumentation.targetContext.packageName
                instrumentation.uiAutomation
                    .executeShellCommand("appops set $packageName MANAGE_EXTERNAL_STORAGE allow")
                    .close()
                base.evaluate()
            }
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(allFilesAccess).around(compose)

    private fun tab(label: String) =
        compose.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    private fun waitForHeading(title: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText(title) and isHeading()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText(title) and isHeading()).assertIsDisplayed()
    }

    @Test
    fun everyTabOpens() {
        waitForHeading("Library")

        tab("Series").performClick()
        waitForHeading("Series")

        tab("Highlights").performClick()
        waitForHeading("Highlights")

        tab("Settings").performClick()
        waitForHeading("Settings")

        tab("Library").performClick()
        waitForHeading("Library")
    }

    @Test
    fun searchOpensFromLibraryAndCloses() {
        waitForHeading("Library")

        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithText("Series, books, authors, genres").assertIsDisplayed()

        compose.onNodeWithContentDescription("Navigate back").performClick()
        waitForHeading("Library")
    }
}
