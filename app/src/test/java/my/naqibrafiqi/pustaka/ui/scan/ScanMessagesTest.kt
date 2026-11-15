package my.naqibrafiqi.pustaka.ui.scan

import my.naqibrafiqi.pustaka.data.library.scan.CompletedScan
import my.naqibrafiqi.pustaka.data.library.scan.ScanOutcome
import my.naqibrafiqi.pustaka.data.library.scan.ScanResult
import my.naqibrafiqi.pustaka.testing.text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ScanMessagesTest {

    private fun finished(result: ScanResult) = ScanOutcome.Finished(result)

    @Test
    fun `new books are counted`() {
        assertEquals("3 new books found", ScanMessages.summary(ScanResult.Success(newBooks = 3)).text())
        assertEquals("1 new book found", ScanMessages.summary(ScanResult.Success(newBooks = 1)).text())
    }

    @Test
    fun `several changes are listed together`() {
        assertEquals(
            "2 new books found, 1 missing book is back, 1 book missing, 1 file couldn't be read",
            ScanMessages.summary(
                ScanResult.Success(newBooks = 2, restoredBooks = 1, missingBooks = 1, unreadableFiles = listOf("/x.epub"))
            ).text(),
        )
    }

    @Test
    fun `new series are only mentioned when no books are new`() {
        assertEquals("1 new series found", ScanMessages.summary(ScanResult.Success(newSeries = 1)).text())
        assertEquals("4 new books found", ScanMessages.summary(ScanResult.Success(newBooks = 4, newSeries = 1)).text())
    }

    @Test
    fun `a scan the user asked for always gets a message`() {
        assertEquals("Library is up to date", ScanMessages.forOutcome(finished(ScanResult.Success()), requestedByUser = true)?.text())
        assertEquals(
            "Can't open the library folder /storage/emulated/0/Pustaka",
            ScanMessages.forOutcome(finished(ScanResult.FolderUnavailable("/storage/emulated/0/Pustaka")), requestedByUser = true)?.text(),
        )
        assertEquals("The scan didn't finish. Try again.", ScanMessages.forOutcome(ScanOutcome.Failed, requestedByUser = true)?.text())
    }

    @Test
    fun `an automatic scan only gets a message when something changed`() {
        assertNull(ScanMessages.forOutcome(finished(ScanResult.Success()), requestedByUser = false))
        assertNull(ScanMessages.forOutcome(finished(ScanResult.FolderUnavailable("/x")), requestedByUser = false))
        assertNull(ScanMessages.forOutcome(ScanOutcome.Failed, requestedByUser = false))
        assertNull(ScanMessages.forOutcome(ScanOutcome.NoAccess, requestedByUser = false))
        assertEquals(
            "1 new book found",
            ScanMessages.forOutcome(finished(ScanResult.Success(newBooks = 1)), requestedByUser = false)?.text(),
        )
    }

    @Test
    fun `scans from before the app started, and scans already reported, are skipped`() {
        val start = Instant.parse("2026-11-14T10:00:00Z")
        val reports = ScanReports(since = start)
        val old = CompletedScan(UUID.randomUUID(), start.minusSeconds(60), ScanOutcome.Failed)
        val current = CompletedScan(UUID.randomUUID(), start.plusSeconds(1), ScanOutcome.Failed)

        assertNull(reports.newlyFinished(old))
        assertNull(reports.newlyFinished(null))
        assertEquals(current, reports.newlyFinished(current))
        assertNull(reports.newlyFinished(current))
    }
}
