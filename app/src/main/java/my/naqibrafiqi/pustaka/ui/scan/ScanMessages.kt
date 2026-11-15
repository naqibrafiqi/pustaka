package my.naqibrafiqi.pustaka.ui.scan

import androidx.annotation.PluralsRes
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.library.scan.ScanOutcome
import my.naqibrafiqi.pustaka.data.library.scan.ScanResult
import my.naqibrafiqi.pustaka.util.text.UiText
import my.naqibrafiqi.pustaka.util.text.pluralText
import my.naqibrafiqi.pustaka.util.text.uiText

/**
 * The short message shown when a scan ends, such as "3 new books found".
 *
 * Scans the user asked for always get a message. Automatic scans (when the app opens or comes back
 * to the foreground) only get one when something changed, so opening the app stays quiet.
 */
object ScanMessages {

    fun forOutcome(outcome: ScanOutcome, requestedByUser: Boolean): UiText? = when (outcome) {
        is ScanOutcome.Finished -> when (val result = outcome.result) {
            is ScanResult.Success -> when {
                result.hasChanges || result.unreadableFiles.isNotEmpty() -> summary(result)
                requestedByUser -> uiText(R.string.scan_up_to_date)
                else -> null
            }
            is ScanResult.FolderUnavailable ->
                if (requestedByUser) uiText(R.string.scan_folder_unavailable, result.folderPath) else null
        }
        ScanOutcome.NoAccess ->
            if (requestedByUser) uiText(R.string.scan_no_access) else null
        ScanOutcome.Failed ->
            if (requestedByUser) uiText(R.string.scan_failed) else null
    }

    /** For example "3 new books found, 1 book missing". */
    fun summary(result: ScanResult.Success): UiText {
        val parts = listOfNotNull(
            count(result.newBooks, R.plurals.scan_new_books),
            count(result.newSeries, R.plurals.scan_new_series)?.takeIf { result.newBooks == 0 },
            count(result.restoredBooks, R.plurals.scan_restored_books),
            count(result.movedBooks, R.plurals.scan_moved_books),
            count(result.updatedBooks, R.plurals.scan_updated_books),
            count(result.missingBooks, R.plurals.scan_missing_books),
            count(result.removedSeries, R.plurals.scan_removed_series),
            count(result.unreadableFiles.size, R.plurals.scan_unreadable_files),
        )
        return if (parts.isEmpty()) uiText(R.string.scan_up_to_date) else UiText.Joined(parts)
    }

    private fun count(n: Int, @PluralsRes id: Int): UiText? = if (n == 0) null else pluralText(id, n)
}
