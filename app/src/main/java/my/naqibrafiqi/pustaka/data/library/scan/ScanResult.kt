package my.naqibrafiqi.pustaka.data.library.scan

/** What a scan of the library folder found. */
sealed interface ScanResult {

    /**
     * @property newBooks files that weren't in the library before.
     * @property movedBooks known books found under a new name or in a new folder. Their progress and
     *   edits are kept.
     * @property updatedBooks known files that changed on disk and were read again.
     * @property restoredBooks books that were missing and whose files are back.
     * @property missingBooks books whose files have gone since the last scan.
     * @property newSeries folders that weren't in the library before.
     * @property removedSeries series whose folder has gone and that have no books left.
     * @property unreadableFiles paths of EPUBs that couldn't be opened. They're still added, with
     *   names from their filenames.
     */
    data class Success(
        val newBooks: Int = 0,
        val movedBooks: Int = 0,
        val updatedBooks: Int = 0,
        val restoredBooks: Int = 0,
        val missingBooks: Int = 0,
        val newSeries: Int = 0,
        val removedSeries: Int = 0,
        val unreadableFiles: List<String> = emptyList(),
    ) : ScanResult {
        val hasChanges: Boolean
            get() = newBooks + movedBooks + updatedBooks + restoredBooks + missingBooks +
                newSeries + removedSeries > 0
    }

    /** The library folder doesn't exist or can't be read, so nothing was changed. */
    data class FolderUnavailable(val folderPath: String) : ScanResult
}
