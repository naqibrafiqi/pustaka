package my.naqibrafiqi.pustaka.data.library.scan

import java.io.File

/**
 * An EPUB found in the library folder.
 *
 * @property seriesFolder the series folder it's in, or null for a standalone book.
 */
internal data class FoundFile(val file: File, val seriesFolder: File?) {
    val path: String get() = file.path
}

/**
 * What's in the library folder, read one level deep:
 * - `.epub` files directly inside are standalone books;
 * - folders directly inside are series, even when they're empty;
 * - `.epub` files inside a series folder are its volumes.
 *
 * Anything deeper, any other kind of file, and anything whose name starts with `.` is ignored.
 *
 * @property unreadableFolders series folders whose contents couldn't be listed. Their books aren't
 *   marked missing, because the files may still be there.
 */
internal class LibraryListing(
    val seriesFolders: List<File>,
    val files: List<FoundFile>,
    val unreadableFolders: Set<String>,
) {
    companion object {

        /** Returns null if [root] isn't a folder or can't be read. */
        fun of(root: File): LibraryListing? {
            if (!root.isDirectory) return null
            val entries = root.listFiles()?.filterNot { it.isIgnored() } ?: return null

            val seriesFolders = entries.filter { it.isDirectory }
            val files = entries.filter { it.isEpub() }.mapTo(mutableListOf()) { FoundFile(it, null) }
            val unreadableFolders = mutableSetOf<String>()
            for (folder in seriesFolders) {
                val volumes = folder.listFiles()
                if (volumes == null) {
                    unreadableFolders += folder.path
                    continue
                }
                volumes.filter { !it.isIgnored() && it.isEpub() }.mapTo(files) { FoundFile(it, folder) }
            }
            return LibraryListing(seriesFolders, files, unreadableFolders)
        }

        private fun File.isIgnored(): Boolean = name.startsWith('.')

        private fun File.isEpub(): Boolean = isFile && name.endsWith(".epub", ignoreCase = true)
    }
}
