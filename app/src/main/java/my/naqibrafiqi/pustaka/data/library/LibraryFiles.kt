package my.naqibrafiqi.pustaka.data.library

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.library.scan.LibraryScans
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import my.naqibrafiqi.pustaka.util.text.UiText
import my.naqibrafiqi.pustaka.util.text.uiText
import java.io.File
import javax.inject.Inject

/** What's in a series folder, for the confirmation before it's deleted. */
data class FolderContents(val files: Int, val epubs: Int)

sealed interface CreateSeriesResult {
    data class Created(val seriesId: Long) : CreateSeriesResult

    /** The name can't be used. [message] says why. */
    data class Invalid(val message: UiText) : CreateSeriesResult

    data class Failed(val message: UiText) : CreateSeriesResult
}

sealed interface DeleteResult {
    data object Deleted : DeleteResult
    data class Failed(val message: UiText) : DeleteResult
}

/**
 * The only changes the app makes to the library folder, each one asked for by the user: creating a
 * series folder, and deleting a series folder or a book's file. See "Deleting" in
 * `docs/local_storage_and_library.md`. Nothing here renames or moves files.
 */
class LibraryFiles @Inject constructor(
    private val settings: SettingsRepository,
    private val seriesRepository: SeriesRepository,
    private val books: BookRepository,
    private val scans: LibraryScans,
) {

    /** Makes an empty folder called [name] in the library folder, and adds it as a series. */
    suspend fun createSeries(name: String): CreateSeriesResult {
        val root = File(settings.settings.first().libraryFolderPath)
        val trimmed = name.trim()
        SeriesNames.problemWith(trimmed)?.let { return CreateSeriesResult.Invalid(it) }

        return withContext(Dispatchers.IO) {
            val folder = File(root, trimmed)
            when {
                !root.isDirectory -> CreateSeriesResult.Failed(uiText(R.string.create_series_no_library_folder, root.path))
                folder.exists() -> CreateSeriesResult.Invalid(uiText(R.string.create_series_exists, trimmed))
                !folder.mkdir() -> CreateSeriesResult.Failed(uiText(R.string.create_series_failed, trimmed))
                else -> {
                    val id = seriesRepository.getSeriesByFolderPath(folder.path)?.id
                        ?: seriesRepository.addSeries(folder.path, folder.name)
                    CreateSeriesResult.Created(id)
                }
            }
        }
    }

    /** Every file in [folderPath], at any depth, and how many of them are EPUBs. */
    suspend fun contentsOf(folderPath: String): FolderContents = withContext(Dispatchers.IO) {
        val files = File(folderPath).walkTopDown().filter { it.isFile }.toList()
        FolderContents(files = files.size, epubs = files.count { it.name.endsWith(".epub", ignoreCase = true) })
    }

    /**
     * Deletes the series' folder with everything in it, then the series and its books from the
     * database. If the folder can't be fully deleted, the database is left alone and a scan sorts
     * out whatever is left.
     */
    suspend fun deleteSeriesFolder(seriesId: Long): DeleteResult {
        val series = seriesRepository.getSeries(seriesId) ?: return DeleteResult.Deleted
        val folder = File(series.folderPath)
        val deleted = withContext(Dispatchers.IO) { !folder.exists() || folder.deleteRecursively() }
        if (!deleted) {
            scans.requestScan()
            return DeleteResult.Failed(uiText(R.string.delete_folder_failed, folder.name))
        }
        seriesRepository.deleteSeries(seriesId)
        return DeleteResult.Deleted
    }

    /** Deletes the book's file, then the book from the database. */
    suspend fun deleteBookFile(bookId: Long): DeleteResult {
        val book = books.getBook(bookId) ?: return DeleteResult.Deleted
        val file = File(book.filePath)
        val deleted = withContext(Dispatchers.IO) { !file.exists() || file.delete() }
        if (!deleted) return DeleteResult.Failed(uiText(R.string.delete_file_failed, file.name))
        books.deleteBook(bookId)
        // The series' details come from its first volume and its cover from its last (or a picked
        // one), and any of those may have been this one.
        if (!book.isStandalone) scans.requestScan()
        return DeleteResult.Deleted
    }
}

/** The rules for a new series folder's name. */
object SeriesNames {

    /** Characters Android's shared storage doesn't allow in file names. */
    private const val NOT_ALLOWED = "\"*/:<>?\\|"
    private const val MAX_LENGTH = 255

    /** Why [name] can't be a folder name, or null if it can. */
    fun problemWith(name: String): UiText? {
        val trimmed = name.trim()
        val bad = trimmed.filter { it in NOT_ALLOWED || it.isISOControl() }.toSet()
        return when {
            trimmed.isEmpty() -> uiText(R.string.series_name_empty)
            trimmed.startsWith('.') -> uiText(R.string.series_name_dot)
            bad.isNotEmpty() -> uiText(R.string.series_name_bad_characters, bad.joinToString(" "))
            trimmed.toByteArray().size > MAX_LENGTH -> uiText(R.string.series_name_too_long)
            else -> null
        }
    }
}
