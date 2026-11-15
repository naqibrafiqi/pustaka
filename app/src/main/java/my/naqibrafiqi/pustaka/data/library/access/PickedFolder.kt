package my.naqibrafiqi.pustaka.data.library.access

/** A folder chosen in Android's folder picker, turned into a file path the scanner can read. */
sealed interface PickedFolder {

    data class Path(val path: String) : PickedFolder

    /** Not on the phone's internal storage, for example an SD card or a cloud drive (decision D15). */
    data object NotInternalStorage : PickedFolder

    companion object {

        /** The authority of Android's own storage provider, the only one with real file paths. */
        const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

        /**
         * Turns a folder from the picker into a path. The picker names folders like
         * `primary:Download/Pustaka`, where `primary` is the internal storage at [internalStorageRoot]
         * (usually `/storage/emulated/0`).
         *
         * @param authority the picker result's authority.
         * @param treeDocumentId the picker result's tree document id.
         */
        fun from(authority: String?, treeDocumentId: String?, internalStorageRoot: String): PickedFolder {
            if (authority != EXTERNAL_STORAGE_AUTHORITY || treeDocumentId == null) return NotInternalStorage
            val volume = treeDocumentId.substringBefore(':')
            if (volume != PRIMARY_VOLUME || ':' !in treeDocumentId) return NotInternalStorage
            val relative = treeDocumentId.substringAfter(':').trim('/')
            val root = internalStorageRoot.trimEnd('/')
            return Path(if (relative.isEmpty()) root else "$root/$relative")
        }

        private const val PRIMARY_VOLUME = "primary"
    }
}
