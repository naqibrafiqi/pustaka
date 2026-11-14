package my.naqibrafiqi.pustaka.data.library.epub

import my.naqibrafiqi.pustaka.data.model.BookDetails
import java.io.File

/**
 * What the scanner learns from opening an EPUB.
 *
 * @property details the details from `content.opf`, with [BookDetails.coverPath] pointing at the
 *   cover saved in the app's cache, if the book has one.
 * @property totalPositions the number of Readium positions, shown as "Page X of Y".
 */
data class EpubMetadata(
    val details: BookDetails,
    val totalPositions: Int?,
)

/** Reads the details of EPUB files. */
interface EpubReader {

    /**
     * Opens [file] and reads its details. The cover is saved under [fingerprint], so a book keeps
     * its cover when the file is renamed.
     *
     * @return null if the file can't be opened as an EPUB, for example when it's damaged.
     */
    suspend fun read(file: File, fingerprint: String): EpubMetadata?
}
