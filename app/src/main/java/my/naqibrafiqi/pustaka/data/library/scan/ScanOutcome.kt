package my.naqibrafiqi.pustaka.data.library.scan

import androidx.work.Data
import androidx.work.WorkInfo
import java.time.Instant
import java.util.UUID

/** How a background scan ended. */
sealed interface ScanOutcome {

    /** The scanner ran. Its result may still be [ScanResult.FolderUnavailable]. */
    data class Finished(val result: ScanResult) : ScanOutcome

    /** The app doesn't have access to the files, so the scan didn't run. */
    data object NoAccess : ScanOutcome

    /** Something went wrong while scanning. */
    data object Failed : ScanOutcome
}

/** A background scan that has ended. */
data class CompletedScan(
    val id: UUID,
    val finishedAt: Instant,
    val outcome: ScanOutcome,
)

data class ScanStatus(
    val isScanning: Boolean,
    val lastScan: CompletedScan?,
) {
    companion object {
        val IDLE = ScanStatus(isScanning = false, lastScan = null)
    }
}

/** Turns a [ScanOutcome] into WorkManager [Data] and back. */
internal object ScanWorkData {

    /** WorkManager limits [Data] to 10 KB, so only the first few unreadable paths are kept. */
    const val MAX_UNREADABLE_PATHS = 20

    private const val OUTCOME = "outcome"
    private const val FINISHED_AT = "finished_at"
    private const val FOLDER_PATH = "folder_path"
    private const val NEW_BOOKS = "new_books"
    private const val MOVED_BOOKS = "moved_books"
    private const val UPDATED_BOOKS = "updated_books"
    private const val RESTORED_BOOKS = "restored_books"
    private const val MISSING_BOOKS = "missing_books"
    private const val NEW_SERIES = "new_series"
    private const val REMOVED_SERIES = "removed_series"
    private const val UNREADABLE_FILES = "unreadable_files"

    private const val SUCCESS = "success"
    private const val FOLDER_UNAVAILABLE = "folder_unavailable"
    private const val NO_ACCESS = "no_access"
    private const val FAILED = "failed"

    fun encode(outcome: ScanOutcome, finishedAt: Instant): Data {
        val builder = Data.Builder().putLong(FINISHED_AT, finishedAt.toEpochMilli())
        when (outcome) {
            ScanOutcome.NoAccess -> builder.putString(OUTCOME, NO_ACCESS)
            ScanOutcome.Failed -> builder.putString(OUTCOME, FAILED)
            is ScanOutcome.Finished -> when (val result = outcome.result) {
                is ScanResult.FolderUnavailable -> builder
                    .putString(OUTCOME, FOLDER_UNAVAILABLE)
                    .putString(FOLDER_PATH, result.folderPath)
                is ScanResult.Success -> builder
                    .putString(OUTCOME, SUCCESS)
                    .putInt(NEW_BOOKS, result.newBooks)
                    .putInt(MOVED_BOOKS, result.movedBooks)
                    .putInt(UPDATED_BOOKS, result.updatedBooks)
                    .putInt(RESTORED_BOOKS, result.restoredBooks)
                    .putInt(MISSING_BOOKS, result.missingBooks)
                    .putInt(NEW_SERIES, result.newSeries)
                    .putInt(REMOVED_SERIES, result.removedSeries)
                    .putStringArray(UNREADABLE_FILES, result.unreadableFiles.take(MAX_UNREADABLE_PATHS).toTypedArray())
            }
        }
        return builder.build()
    }

    /** Returns null for data that wasn't written by [encode], for example after a cancelled scan. */
    fun decode(data: Data): Pair<Instant, ScanOutcome>? {
        val finishedAt = data.getLong(FINISHED_AT, -1).takeIf { it >= 0 } ?: return null
        val outcome = when (data.getString(OUTCOME)) {
            NO_ACCESS -> ScanOutcome.NoAccess
            FAILED -> ScanOutcome.Failed
            FOLDER_UNAVAILABLE -> ScanOutcome.Finished(
                ScanResult.FolderUnavailable(data.getString(FOLDER_PATH).orEmpty())
            )
            SUCCESS -> ScanOutcome.Finished(
                ScanResult.Success(
                    newBooks = data.getInt(NEW_BOOKS, 0),
                    movedBooks = data.getInt(MOVED_BOOKS, 0),
                    updatedBooks = data.getInt(UPDATED_BOOKS, 0),
                    restoredBooks = data.getInt(RESTORED_BOOKS, 0),
                    missingBooks = data.getInt(MISSING_BOOKS, 0),
                    newSeries = data.getInt(NEW_SERIES, 0),
                    removedSeries = data.getInt(REMOVED_SERIES, 0),
                    unreadableFiles = data.getStringArray(UNREADABLE_FILES)?.toList().orEmpty(),
                )
            )
            else -> return null
        }
        return Instant.ofEpochMilli(finishedAt) to outcome
    }

    /** The status of the scan job, from WorkManager's records of it. */
    fun status(infos: List<WorkInfo>): ScanStatus = ScanStatus(
        isScanning = infos.any { !it.state.isFinished },
        lastScan = infos
            .filter { it.state.isFinished }
            .mapNotNull { info -> decode(info.outputData)?.let { (at, outcome) -> CompletedScan(info.id, at, outcome) } }
            .maxByOrNull { it.finishedAt },
    )
}
