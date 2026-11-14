package my.naqibrafiqi.pustaka.ui.feature.reader

import my.naqibrafiqi.pustaka.testing.T0
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SessionTrackerTest {

    private val tracker = SessionTracker()

    private fun at(minutes: Long, seconds: Long = 0): Instant = T0.plusSeconds(minutes * 60 + seconds)

    @Test
    fun countsThePagesTurnedUntilTheReaderStops() {
        tracker.onPage(10, at(0))
        tracker.onPage(11, at(1))
        tracker.onPage(12, at(2))

        assertEquals(TrackedSession(at(0), at(3), pagesRead = 2), tracker.stop(at(3)))
        assertFalse(tracker.isActive)
    }

    @Test
    fun goingBackOverTheSamePagesDoesntCountThemTwice() {
        tracker.onPage(10, at(0))
        tracker.onPage(11, at(1))
        tracker.onPage(10, at(2))
        tracker.onPage(11, at(3))

        assertEquals(1, tracker.stop(at(4))?.pagesRead)
    }

    @Test
    fun aSessionWithoutPageTurnsIsntRecorded() {
        tracker.onPage(10, at(0))

        assertNull(tracker.stop(at(3)))
    }

    @Test
    fun stoppingWithoutReadingRecordsNothing() {
        assertNull(tracker.stop(at(1)))
    }

    @Test
    fun aBookLeftOpenEndsTheSessionAtTheLastPageTurn() {
        tracker.onPage(10, at(0))
        tracker.onPage(11, at(2))

        // Put down for an hour, then the app goes into the background.
        assertEquals(TrackedSession(at(0), at(2), pagesRead = 1), tracker.stop(at(62)))
    }

    @Test
    fun turningAPageAfterABreakStartsANewSession() {
        tracker.onPage(10, at(0))
        tracker.onPage(11, at(2))

        val ended = tracker.onPage(12, at(30))

        assertEquals(TrackedSession(at(0), at(2), pagesRead = 1), ended)
        assertTrue(tracker.isActive)
        tracker.onPage(13, at(31))
        assertEquals(TrackedSession(at(30), at(32), pagesRead = 1), tracker.stop(at(32)))
    }

    @Test
    fun aShortPauseDoesntSplitTheSession() {
        tracker.onPage(10, at(0))
        assertNull(tracker.onPage(11, at(4, 59)))
        assertEquals(TrackedSession(at(0), at(5), pagesRead = 1), tracker.stop(at(5)))
    }
}
