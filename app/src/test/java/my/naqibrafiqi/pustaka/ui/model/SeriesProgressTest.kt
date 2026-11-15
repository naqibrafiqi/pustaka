package my.naqibrafiqi.pustaka.ui.model

import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.testing.T0
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.testing.text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeriesProgressTest {

    private fun volume(
        number: Int,
        status: ReadingStatus = ReadingStatus.UNREAD,
        openedMinutesAgo: Long? = null,
        missing: Boolean = false,
    ): Book = book("Vol $number.epub", seriesId = 1, id = number.toLong(), volumeInFile = number.toDouble())
        .copy(
            readingStatus = status,
            lastOpenedAt = openedMinutesAgo?.let { T0.minusSeconds(it * 60) },
            isMissing = missing,
        )

    @Test
    fun countsFinishedVolumesAndRoundsThePercentDown() {
        val progress = SeriesProgress(
            listOf(volume(1, ReadingStatus.FINISHED), volume(2, ReadingStatus.READING), volume(3)),
        )

        assertEquals(3, progress.total)
        assertEquals(1, progress.finished)
        assertEquals(33, progress.percent)
    }

    @Test
    fun anEmptySeriesHasNoCurrentVolume() {
        val progress = SeriesProgress(emptyList())

        assertEquals(0, progress.percent)
        assertNull(progress.current)
        assertEquals("No volumes yet", progress.currentLabel.text())
        assertEquals(emptyList<Book>(), progress.preview())
    }

    @Test
    fun theVolumeBeingReadThatWasOpenedLastComesFirst() {
        val progress = SeriesProgress(
            listOf(
                volume(1, ReadingStatus.READING, openedMinutesAgo = 60),
                volume(2, ReadingStatus.READING, openedMinutesAgo = 5),
                volume(3),
            ),
        )

        assertEquals(2L, progress.current?.id)
        assertEquals("Reading: Volume 2", progress.currentLabel.text())
    }

    @Test
    fun withNothingBeingReadTheFirstUnfinishedVolumeIsNext() {
        val progress = SeriesProgress(
            listOf(volume(1, ReadingStatus.FINISHED), volume(2, ReadingStatus.FINISHED), volume(3), volume(4)),
        )

        assertEquals(3L, progress.current?.id)
        assertEquals("Up next: Volume 3", progress.currentLabel.text())
    }

    @Test
    fun missingVolumesAreSkipped() {
        val progress = SeriesProgress(
            listOf(volume(1, ReadingStatus.READING, openedMinutesAgo = 1, missing = true), volume(2)),
        )

        assertEquals(2L, progress.current?.id)
    }

    @Test
    fun aFinishedSeriesSaysSo() {
        val progress = SeriesProgress(listOf(volume(1, ReadingStatus.FINISHED), volume(2, ReadingStatus.FINISHED)))

        assertNull(progress.current)
        assertEquals(100, progress.percent)
        assertEquals("All volumes read", progress.currentLabel.text())
    }

    @Test
    fun volumesWithoutANumberAreLabelledByTheirPlace() {
        val first = book("Prologue.epub", seriesId = 1, id = 1)
        val second = book("Epilogue.epub", seriesId = 1, id = 2)
        val numbered = book("Side story.epub", seriesId = 1, id = 3, volumeInFile = 4.5)
        val progress = SeriesProgress(listOf(first, second, numbered))

        assertEquals("Volume 1", progress.labelOf(first).text())
        assertEquals("Volume 2", progress.labelOf(second).text())
        assertEquals("Volume 4.5", progress.labelOf(numbered).text())
    }

    @Test
    fun thePreviewShowsTheVolumesAroundTheCurrentOne() {
        val volumes = (1..6).map { n ->
            volume(n, if (n < 4) ReadingStatus.FINISHED else ReadingStatus.UNREAD)
        }

        assertEquals(listOf(3L, 4L, 5L), SeriesProgress(volumes).preview().map { it.id })
    }

    @Test
    fun thePreviewStaysInsideTheSeries() {
        val atStart = (1..5).map { volume(it) }
        val finished = (1..5).map { volume(it, ReadingStatus.FINISHED) }

        assertEquals(listOf(1L, 2L, 3L), SeriesProgress(atStart).preview().map { it.id })
        assertEquals(listOf(3L, 4L, 5L), SeriesProgress(finished).preview().map { it.id })
    }

    @Test
    fun volumeNumbersDropTrailingZeros() {
        assertEquals("4", formatVolumeNumber(4.0))
        assertEquals("4.5", formatVolumeNumber(4.5))
        assertEquals("10", formatVolumeNumber(10.0))
    }
}
