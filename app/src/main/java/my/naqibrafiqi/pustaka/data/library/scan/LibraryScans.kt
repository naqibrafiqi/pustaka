package my.naqibrafiqi.pustaka.data.library.scan

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Starts library scans and reports on them. Scans run when the app is opened or comes back to the
 * foreground, when the user pulls to refresh, and from Settings.
 */
interface LibraryScans {

    val status: Flow<ScanStatus>

    /** Starts a scan, unless one is already waiting or running. */
    fun requestScan()
}

class WorkManagerLibraryScans @Inject constructor(
    private val workManager: WorkManager,
) : LibraryScans {

    override val status: Flow<ScanStatus> = workManager
        .getWorkInfosForUniqueWorkFlow(WORK_NAME)
        .map(ScanWorkData::status)
        .distinctUntilChanged()

    override fun requestScan() {
        workManager.enqueueUniqueWork(
            WORK_NAME,
            // A scan that's already queued or running will see the same files.
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<LibraryScanWorker>().build(),
        )
    }

    private companion object {
        const val WORK_NAME = "library-scan"
    }
}
