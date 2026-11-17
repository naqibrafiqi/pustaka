package my.naqibrafiqi.pustaka.ui.feature.reader

import my.naqibrafiqi.pustaka.ui.feature.reader.ReturnTracker.Companion.PAGES_TO_FORGET
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnTrackerTest {

    private val tracker = ReturnTracker<String>()

    @Test
    fun aJumpRemembersThePageItLeft() {
        tracker.onJump("page 42", fromPage = 42, toPage = 120)

        assertEquals(ReturnPoint("page 42", 42), tracker.point)
    }

    @Test
    fun goingBackForgetsThePage() {
        tracker.onJump("page 42", fromPage = 42, toPage = 120)

        assertEquals("page 42", tracker.take())
        assertNull(tracker.point)
        assertNull(tracker.take())
    }

    @Test
    fun readingTenPagesOnFromTheJumpForgetsThePage() {
        tracker.onJump("page 42", fromPage = 42, toPage = 120)
        tracker.onPage(120)

        for (page in 121 until 120 + PAGES_TO_FORGET) assertFalse(tracker.onPage(page))
        assertTrue(tracker.onPage(120 + PAGES_TO_FORGET))
        assertNull(tracker.point)
    }

    @Test
    fun lookingBackFromTheJumpDoesntCountAsReadingOn() {
        tracker.onJump("page 42", fromPage = 42, toPage = 120)

        tracker.onPage(110)
        tracker.onPage(125)

        assertEquals(42, tracker.point?.page)
    }

    @Test
    fun withoutTheLandingPageItCountsFromTheFirstPageShown() {
        tracker.onJump("page 42", fromPage = 42, toPage = null)
        tracker.onPage(200)

        assertFalse(tracker.onPage(200 + PAGES_TO_FORGET - 1))
        assertTrue(tracker.onPage(200 + PAGES_TO_FORGET))
    }

    @Test
    fun anotherJumpKeepsThePageBeingReadAndCountsFromTheNewSpot() {
        tracker.onJump("page 42", fromPage = 42, toPage = 120)
        tracker.onPage(125)
        tracker.onJump("page 125", fromPage = 125, toPage = 300)

        assertFalse(tracker.onPage(300 + PAGES_TO_FORGET - 1))
        assertEquals(ReturnPoint("page 42", 42), tracker.point)
    }

    @Test
    fun turningBackToTheRememberedPageForgetsIt() {
        tracker.onJump("page 42", fromPage = 42, toPage = 45)

        assertTrue(tracker.onPage(42))
        assertNull(tracker.point)
    }

    @Test
    fun jumpingToTheSamePageRemembersNothing() {
        tracker.onJump("page 42", fromPage = 42, toPage = 42)

        assertNull(tracker.point)
    }
}
