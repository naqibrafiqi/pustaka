package my.naqibrafiqi.pustaka.data.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.naqibrafiqi.pustaka.data.library.epub.CoverStore
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import java.io.File
import java.util.Locale
import javax.inject.Inject

/** How much space the library and the app take up, for Settings → Storage. */
data class StorageReport(
    /** The EPUB files the library has found, wherever they are. Missing books aren't counted. */
    val bookFiles: Long,
    val bookCount: Int,
    /** The cover images kept in the app's cache. */
    val covers: Long,
    /** The database and settings: progress, highlights, sessions and edits. */
    val appData: Long,
)

interface StorageUsage {

    suspend fun measure(): StorageReport

    /** Deletes the cached covers. They come back on the next scan, which reads each book again. */
    suspend fun clearCovers()
}

class DefaultStorageUsage @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: PustakaDatabase,
    private val coverStore: CoverStore,
) : StorageUsage {

    override suspend fun measure(): StorageReport = withContext(Dispatchers.IO) {
        val onDisk = database.bookDao().getAll().filterNot { it.isMissing }
        StorageReport(
            bookFiles = onDisk.sumOf { it.fileSize },
            bookCount = onDisk.size,
            covers = coverStore.sizeInBytes(),
            appData = sizeOf(context.getDatabasePath(PustakaDatabase.NAME).parentFile) +
                sizeOf(File(context.filesDir, "datastore")),
        )
    }

    override suspend fun clearCovers() = withContext(Dispatchers.IO) { coverStore.clear() }

    private fun sizeOf(file: File?): Long = file?.walkBottomUp()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
}

/** "0 B", "980 B", "12.3 KB", "4.5 MB", "1.2 GB", using steps of 1,000 as Android does. */
fun formatBytes(bytes: Long): String {
    if (bytes < UNIT) return "$bytes B"
    var value = bytes.toDouble()
    var unit = 0
    while (value >= UNIT && unit < UNITS.lastIndex) {
        value /= UNIT
        unit++
    }
    val number = if (value >= 100) "%.0f".format(Locale.ROOT, value) else "%.1f".format(Locale.ROOT, value)
    return "$number ${UNITS[unit]}"
}

private const val UNIT = 1000.0
private val UNITS = listOf("B", "KB", "MB", "GB", "TB")
