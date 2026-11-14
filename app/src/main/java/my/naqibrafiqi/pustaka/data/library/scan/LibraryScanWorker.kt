package my.naqibrafiqi.pustaka.data.library.scan

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import my.naqibrafiqi.pustaka.data.library.access.StorageAccess
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import java.io.File
import java.time.Clock

/**
 * Scans the library in the background, so a large scan keeps going when the user leaves the app.
 *
 * It creates the library folder first if it doesn't exist, as long as the app has access to the
 * files. The outcome is saved in the work's output, see [ScanWorkData].
 */
@HiltWorker
class LibraryScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scanner: LibraryScanner,
    private val storageAccess: StorageAccess,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!storageAccess.hasAccess()) {
            return Result.failure(ScanWorkData.encode(ScanOutcome.NoAccess, clock.instant()))
        }
        return try {
            val folder = File(settings.settings.first().libraryFolderPath)
            if (!folder.exists()) folder.mkdirs()
            val result = scanner.scan()
            Result.success(ScanWorkData.encode(ScanOutcome.Finished(result), clock.instant()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Library scan failed", e)
            Result.failure(ScanWorkData.encode(ScanOutcome.Failed, clock.instant()))
        }
    }

    private companion object {
        const val TAG = "LibraryScanWorker"
    }
}
