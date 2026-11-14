package my.naqibrafiqi.pustaka.ui.model

import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.Creator
import my.naqibrafiqi.pustaka.data.model.ReadingProgress
import my.naqibrafiqi.pustaka.data.model.ReadingSession
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.testing.T0
import my.naqibrafiqi.pustaka.testing.book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.ZoneOffset

class PresentationTest {

    private fun progress(fraction: Double) =
        ReadingProgress(bookId = 1, locator = "{}", progression = fraction, position = 10, updatedAt = T0)

    private fun session(minutes: Long, pages: Int) =
        ReadingSession(bookId = 1, startedAt = T0, endedAt = T0.plusSeconds(minutes * 60), pagesRead = pages)

    // --- Book cards ---

    @Test
    fun theStateOfAMissingBookIsMissingWhateverItsStatus() {
        val missing = book("a.epub").copy(isMissing = true, readingStatus = ReadingStatus.READING)

        assertEquals(BookState.MISSING, BookState.of(missing))
        assertEquals(BookState.READING, BookState.of(missing.copy(isMissing = false)))
    }

    @Test
    fun percentReadFollowsTheReadingStatus() {
        val book = book("a.epub")

        assertEquals(0, book.copy(readingStatus = ReadingStatus.UNREAD).percentRead(progress(0.5)))
        assertEquals(50, book.copy(readingStatus = ReadingStatus.READING).percentRead(progress(0.5)))
        assertEquals(0, book.copy(readingStatus = ReadingStatus.READING).percentRead(null))
        assertEquals(100, book.copy(readingStatus = ReadingStatus.FINISHED).percentRead(progress(0.97)))
    }

    @Test
    fun theCreditLineListsAuthorsThenIllustratorsOnce() {
        val details = BookDetails(
            creators = listOf(
                Creator("Nekometaru", Creator.ROLE_ILLUSTRATOR),
                Creator("Yushi Ukai"),
                Creator("Someone", Creator.ROLE_TRANSLATOR),
                Creator("Yushi Ukai", Creator.ROLE_AUTHOR),
            ),
        )

        assertEquals("Yushi Ukai, Nekometaru", details.creditLine())
        assertNull(BookDetails.EMPTY.creditLine())
    }

    @Test
    fun pageLabelsUseWhatIsKnown() {
        assertEquals("Page 116 of 200", pageLabel(116, 200))
        assertEquals("Page 116", pageLabel(116, null))
        assertEquals("200 pages", pageLabel(null, 200))
        assertEquals("1 page", pageLabel(null, 1))
        assertNull(pageLabel(null, null))
    }

    // --- Reading stats ---

    @Test
    fun velocityNeedsAMinuteOfReadingWithPagesTurned() {
        assertNull(ReadingStats(emptyList()).pagesPerHour)
        assertNull(ReadingStats(listOf(session(minutes = 0, pages = 5))).pagesPerHour)
        assertNull(ReadingStats(listOf(session(minutes = 30, pages = 0))).pagesPerHour)
        assertEquals("—", velocityLabel(null))
    }

    @Test
    fun velocityIsPagesPerHourOverAllSessions() {
        val stats = ReadingStats(listOf(session(minutes = 30, pages = 20), session(minutes = 30, pages = 40)))

        assertEquals(60.0, stats.pagesPerHour!!, 0.001)
        assertEquals("Avg. 60 pages/h", velocityLabel(stats.pagesPerHour))
    }

    @Test
    fun timeLeftIsRoundedUpToTheMinute() {
        val stats = ReadingStats(listOf(session(minutes = 60, pages = 60)))

        assertEquals(Duration.ofMinutes(135), stats.timeLeft(135))
        assertEquals("~2h 15m", timeLeftLabel(stats.timeLeft(135)))
        assertEquals("~45m", timeLeftLabel(stats.timeLeft(45)))
        assertEquals("~2h", timeLeftLabel(stats.timeLeft(120)))
        assertEquals("<1m", timeLeftLabel(stats.timeLeft(0)))
        assertEquals("—", timeLeftLabel(ReadingStats(emptyList()).timeLeft(10)))
    }

    @Test
    fun readAgoGetsCoarserWithTime() {
        val now = T0

        assertNull(readAgoLabel(null, now))
        assertEquals("Read just now", readAgoLabel(now.minusSeconds(30), now))
        assertEquals("Read 32m ago", readAgoLabel(now.minusSeconds(32 * 60), now))
        assertEquals("Read 3h ago", readAgoLabel(now.minusSeconds(3 * 3600 + 120), now))
        assertEquals("Read 2d ago", readAgoLabel(now.minus(Duration.ofDays(2)), now))
        assertEquals("Read on 2 Oct 2026", readAgoLabel(now.minus(Duration.ofDays(30)), now, ZoneOffset.UTC))
        assertEquals("Read just now", readAgoLabel(now.plusSeconds(60), now))
    }

    @Test
    fun fileSizesUseTheLargestFittingUnit() {
        assertEquals("0 B", fileSizeLabel(0))
        assertEquals("820 KB", fileSizeLabel(820 * 1024 + 100))
        assertEquals("14.2 MB", fileSizeLabel((14.2 * 1024 * 1024).toLong()))
    }
}
