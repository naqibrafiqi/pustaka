package my.naqibrafiqi.pustaka.ui.feature.reader

import my.naqibrafiqi.pustaka.ui.feature.reader.ReturnTracker.Companion.TURNS_TO_FORGET
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnTrackerTest {

    private val tracker = ReturnTracker<String>()

    /** Jumps from page 42 and lands on page 120. */
    private fun jump() {
        tracker.onJump("page 42", fromPage = 42)
        tracker.onMove(page = 120, turns = 1)
    }

    @Test
    fun aJumpRemembersThePageItLeft() {
        jump()

        assertEquals(ReturnPoint("page 42", 42), tracker.point)
    }

    @Test
    fun goingBackForgetsThePage() {
        jump()

        assertEquals("page 42", tracker.take())
        assertNull(tracker.point)
        assertNull(tracker.take())
    }

    @Test
    fun tenTurnsForwardFromTheJumpForgetThePage() {
        jump()

        // Several screens can share a page number, so turns are counted, not pages.
        repeat(TURNS_TO_FORGET - 1) { assertFalse(tracker.onMove(page = 121, turns = 1)) }
        assertTrue(tracker.onMove(page = 122, turns = 1))
        assertNull(tracker.point)
    }

    @Test
    fun turningBackTakesATurnOff() {
        jump()

        repeat(TURNS_TO_FORGET - 1) { tracker.onMove(page = null, turns = 1) }
        tracker.onMove(page = null, turns = -1)
        assertFalse(tracker.onMove(page = null, turns = 1))
        assertTrue(tracker.onMove(page = null, turns = 1))
    }

    @Test
    fun scrollingCountsThePagesScrolled() {
        jump()

        assertFalse(tracker.onMove(page = 129, turns = TURNS_TO_FORGET - 1))
        assertTrue(tracker.onMove(page = 130, turns = 1))
    }

    @Test
    fun theLandingIsntATurn() {
        tracker.onJump("page 42", fromPage = 42)

        assertFalse(tracker.onMove(page = 200, turns = TURNS_TO_FORGET))
        assertEquals(42, tracker.point?.page)
    }

    @Test
    fun anotherJumpKeepsThePageBeingReadAndCountsFromTheNewSpot() {
        jump()
        repeat(5) { tracker.onMove(page = null, turns = 1) }
        tracker.onJump("page 125", fromPage = 125)
        tracker.onMove(page = 300, turns = 1)

        repeat(TURNS_TO_FORGET - 1) { assertFalse(tracker.onMove(page = null, turns = 1)) }
        assertEquals(ReturnPoint("page 42", 42), tracker.point)
    }

    @Test
    fun turningBackToTheRememberedPageForgetsIt() {
        jump()

        assertTrue(tracker.onMove(page = 42, turns = -1))
        assertNull(tracker.point)
    }

    @Test
    fun landingOnTheSamePageRemembersNothing() {
        tracker.onJump("page 42", fromPage = 42)

        assertTrue(tracker.onMove(page = 42, turns = 0))
        assertNull(tracker.point)
    }
}
