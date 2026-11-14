package my.naqibrafiqi.pustaka.data.library.scan

import androidx.work.Data
import androidx.work.WorkInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ScanWorkDataTest {

    private val at = Instant.parse("2026-11-14T10:00:00Z")

    @Test
    fun `every outcome survives the round trip`() {
        val outcomes = listOf(
            ScanOutcome.Finished(
                ScanResult.Success(
                    newBooks = 3, movedBooks = 1, updatedBooks = 2, restoredBooks = 4,
                    missingBooks = 5, newSeries = 6, removedSeries = 7,
                    unreadableFiles = listOf("/a.epub", "/b.epub"),
                )
            ),
            ScanOutcome.Finished(ScanResult.Success()),
            ScanOutcome.Finished(ScanResult.FolderUnavailable("/storage/emulated/0/Pustaka")),
            ScanOutcome.NoAccess,
            ScanOutcome.Failed,
        )
        for (outcome in outcomes) {
            assertEquals(at to outcome, ScanWorkData.decode(ScanWorkData.encode(outcome, at)))
        }
    }

    @Test
    fun `only the first unreadable paths are kept`() {
        val paths = (1..100).map { "/storage/emulated/0/Pustaka/Book $it.epub" }
        val data = ScanWorkData.encode(ScanOutcome.Finished(ScanResult.Success(unreadableFiles = paths)), at)

        val result = (ScanWorkData.decode(data)!!.second as ScanOutcome.Finished).result as ScanResult.Success

        assertEquals(paths.take(ScanWorkData.MAX_UNREADABLE_PATHS), result.unreadableFiles)
    }

    @Test
    fun `data from elsewhere decodes to null`() {
        assertNull(ScanWorkData.decode(Data.EMPTY))
    }

    @Test
    fun `status reports running work and the latest finished scan`() {
        val older = workInfo(WorkInfo.State.SUCCEEDED, ScanWorkData.encode(ScanOutcome.Failed, at))
        val newer = workInfo(
            WorkInfo.State.SUCCEEDED,
            ScanWorkData.encode(ScanOutcome.Finished(ScanResult.Success(newBooks = 1)), at.plusSeconds(5)),
        )
        val cancelled = workInfo(WorkInfo.State.CANCELLED, Data.EMPTY)

        val idle = ScanWorkData.status(listOf(older, newer, cancelled))
        assertFalse(idle.isScanning)
        assertEquals(newer.id, idle.lastScan!!.id)
        assertEquals(at.plusSeconds(5), idle.lastScan!!.finishedAt)

        val running = ScanWorkData.status(listOf(workInfo(WorkInfo.State.RUNNING, Data.EMPTY)))
        assertTrue(running.isScanning)
        assertNull(running.lastScan)

        assertEquals(ScanStatus.IDLE, ScanWorkData.status(emptyList()))
    }

    private fun workInfo(state: WorkInfo.State, output: Data) =
        WorkInfo(UUID.randomUUID(), state, emptySet(), output)
}
