package my.naqibrafiqi.pustaka.ui.scan

import my.naqibrafiqi.pustaka.data.library.scan.ScanOutcome
import my.naqibrafiqi.pustaka.data.library.scan.ScanResult

/**
 * The short message shown when a scan ends, such as "3 new books found".
 *
 * Scans the user asked for always get a message. Automatic scans (when the app opens or comes back
 * to the foreground) only get one when something changed, so opening the app stays quiet.
 */
object ScanMessages {

    fun forOutcome(outcome: ScanOutcome, requestedByUser: Boolean): String? = when (outcome) {
        is ScanOutcome.Finished -> when (val result = outcome.result) {
            is ScanResult.Success -> when {
                result.hasChanges || result.unreadableFiles.isNotEmpty() -> summary(result)
                requestedByUser -> "Library is up to date"
                else -> null
            }
            is ScanResult.FolderUnavailable ->
                if (requestedByUser) "Can't open the library folder ${result.folderPath}" else null
        }
        ScanOutcome.NoAccess ->
            if (requestedByUser) "Pustaka needs access to your files to scan the library" else null
        ScanOutcome.Failed ->
            if (requestedByUser) "The scan didn't finish. Try again." else null
    }

    /** For example "3 new books found, 1 book missing". */
    fun summary(result: ScanResult.Success): String {
        val parts = listOfNotNull(
            count(result.newBooks, "new book", "new books")?.let { "$it found" },
            count(result.newSeries, "new series", "new series")?.takeIf { result.newBooks == 0 }?.let { "$it found" },
            count(result.restoredBooks, "missing book is back", "missing books are back"),
            count(result.movedBooks, "book renamed or moved", "books renamed or moved"),
            count(result.updatedBooks, "book updated", "books updated"),
            count(result.missingBooks, "book missing", "books missing"),
            count(result.removedSeries, "series removed", "series removed"),
            count(result.unreadableFiles.size, "file couldn't be read", "files couldn't be read"),
        )
        return parts.joinToString(", ").ifEmpty { "Library is up to date" }
    }

    private fun count(n: Int, one: String, many: String): String? = when (n) {
        0 -> null
        1 -> "1 $one"
        else -> "$n $many"
    }
}
