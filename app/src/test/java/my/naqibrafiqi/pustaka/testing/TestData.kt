package my.naqibrafiqi.pustaka.testing

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.BookDetails
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

val T0: Instant = Instant.parse("2026-11-01T10:00:00Z")

/** A clock that only moves when a test moves it. */
class TestClock(var now: Instant = T0) : Clock() {
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    fun advanceSeconds(seconds: Long) {
        now = now.plusSeconds(seconds)
    }
}

fun inMemoryDatabase(): PustakaDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        PustakaDatabase::class.java,
    ).allowMainThreadQueries().build()

fun book(
    fileName: String,
    seriesId: Long? = null,
    folder: String = "/storage/emulated/0/Pustaka",
    id: Long = 0,
    volumeInFile: Double? = null,
    editedVolume: Double? = null,
    addedAt: Instant = T0,
) = Book(
    id = id,
    seriesId = seriesId,
    filePath = "$folder/$fileName",
    fileName = fileName,
    fingerprint = "fp-$folder/$fileName",
    fileSize = 1_000,
    fileLastModified = T0,
    sourceDetails = BookDetails(volumeNumber = volumeInFile),
    editedDetails = BookDetails(volumeNumber = editedVolume),
    addedAt = addedAt,
)
