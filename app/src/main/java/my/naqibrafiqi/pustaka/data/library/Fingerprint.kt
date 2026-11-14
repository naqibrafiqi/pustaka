package my.naqibrafiqi.pustaka.data.library

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Identifies a book file by its contents, so the app recognises it after it's been renamed or
 * moved.
 *
 * The fingerprint is a SHA-256 hash of the file's size plus its first and last 64 KB. Reading only
 * those parts keeps it fast for large files, and an EPUB's last bytes hold the ZIP directory, which
 * lists every file in the book with its checksum, so two different books are very unlikely to
 * share a fingerprint.
 */
object Fingerprint {

    const val CHUNK_SIZE = 64 * 1024

    fun of(file: File): String = RandomAccessFile(file, "r").use { input ->
        val size = input.length()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(size).array())

        val head = ByteArray(minOf(CHUNK_SIZE.toLong(), size).toInt())
        input.readFully(head)
        digest.update(head)

        // The tail starts after the head, so a small file isn't hashed twice.
        val tailStart = maxOf(CHUNK_SIZE.toLong(), size - CHUNK_SIZE)
        if (size > tailStart) {
            val tail = ByteArray((size - tailStart).toInt())
            input.seek(tailStart)
            input.readFully(tail)
            digest.update(tail)
        }

        digest.digest().joinToString("") { "%02x".format(it) }
    }
}
