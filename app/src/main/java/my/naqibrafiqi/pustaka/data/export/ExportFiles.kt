package my.naqibrafiqi.pustaka.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

/** Files made for sharing, such as exported highlights (decision D11). */
interface ExportFiles {

    /**
     * Writes [text] to a file called [fileName] that other apps can be given.
     *
     * @return the file's `content://` address, or null if it couldn't be written.
     */
    suspend fun write(fileName: String, text: String): Uri?
}

/**
 * Writes exports to the app's cache, never to the library folder, and keeps only the latest one.
 * Other apps read them through the `FileProvider` declared in the manifest.
 */
class CacheExportFiles @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ExportFiles {

    override suspend fun write(fileName: String, text: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val folder = File(context.cacheDir, FOLDER)
            folder.deleteRecursively()
            folder.mkdirs()
            val file = File(folder, fileName)
            file.writeText(text)
            FileProvider.getUriForFile(context, "${context.packageName}.exports", file)
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        /** Must match `res/xml/export_paths.xml`. */
        const val FOLDER = "exports"
    }
}
