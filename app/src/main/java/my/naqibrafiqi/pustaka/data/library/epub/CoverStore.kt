package my.naqibrafiqi.pustaka.data.library.epub

import android.graphics.Bitmap
import java.io.File

/**
 * Keeps book covers as JPEG files in the app's cache, named after the book's fingerprint.
 *
 * The cache can be cleared by the system or the user. The scanner notices a missing cover and reads
 * the book again, so covers come back on the next scan.
 */
class CoverStore(private val directory: File) {

    /** Saves [cover] for the book with [fingerprint] and returns the file's path. */
    fun save(fingerprint: String, cover: Bitmap): String? {
        if (!directory.isDirectory && !directory.mkdirs()) return null
        val target = File(directory, "$fingerprint.$EXTENSION")
        val partial = File(directory, "$fingerprint.$EXTENSION.partial")
        val written = partial.outputStream().use { cover.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        if (!written || !partial.renameTo(target)) {
            partial.delete()
            return null
        }
        return target.path
    }

    /** How much space the covers take up. */
    fun sizeInBytes(): Long = directory.listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L

    /** Deletes every cover. The next scan reads the books again to bring them back. */
    fun clear() = deleteAllExcept(emptySet())

    /** Deletes every cover whose path isn't in [inUse]. */
    fun deleteAllExcept(inUse: Set<String>) {
        directory.listFiles()?.forEach { file ->
            if (file.path !in inUse) file.delete()
        }
    }

    private companion object {
        const val EXTENSION = "jpg"
        const val QUALITY = 90
    }
}
