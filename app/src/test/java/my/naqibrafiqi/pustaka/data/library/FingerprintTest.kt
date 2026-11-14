package my.naqibrafiqi.pustaka.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random

class FingerprintTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun file(name: String, bytes: ByteArray): File = temp.newFile(name).apply { writeBytes(bytes) }

    @Test
    fun `the same contents give the same fingerprint whatever the name`() {
        val bytes = Random(1).nextBytes(300_000)
        assertEquals(Fingerprint.of(file("a.epub", bytes)), Fingerprint.of(file("renamed.epub", bytes)))
    }

    @Test
    fun `a change in the first or last 64 KB changes the fingerprint`() {
        val bytes = Random(2).nextBytes(300_000)
        val original = Fingerprint.of(file("original.epub", bytes))

        val headChanged = bytes.copyOf().also { it[10] = (it[10] + 1).toByte() }
        val tailChanged = bytes.copyOf().also { it[it.size - 10] = (it[it.size - 10] + 1).toByte() }

        assertNotEquals(original, Fingerprint.of(file("head.epub", headChanged)))
        assertNotEquals(original, Fingerprint.of(file("tail.epub", tailChanged)))
    }

    @Test
    fun `the size is part of the fingerprint`() {
        val bytes = Random(3).nextBytes(1_000)
        assertNotEquals(
            Fingerprint.of(file("short.epub", bytes)),
            Fingerprint.of(file("long.epub", bytes + ByteArray(1))),
        )
    }

    @Test
    fun `small and empty files work`() {
        val empty = Fingerprint.of(file("empty.epub", ByteArray(0)))
        val tiny = Fingerprint.of(file("tiny.epub", byteArrayOf(1, 2, 3)))
        val exactlyOneChunk = Fingerprint.of(file("chunk.epub", Random(4).nextBytes(Fingerprint.CHUNK_SIZE)))

        assertEquals(64, empty.length)
        assertNotEquals(empty, tiny)
        assertEquals(64, exactlyOneChunk.length)
    }

    @Test
    fun `files between one and two chunks are hashed without overlap`() {
        val bytes = Random(5).nextBytes(Fingerprint.CHUNK_SIZE + 100)
        val changedAtEnd = bytes.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertNotEquals(Fingerprint.of(file("a.epub", bytes)), Fingerprint.of(file("b.epub", changedAtEnd)))
    }
}
