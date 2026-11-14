package my.naqibrafiqi.pustaka.data.library

import my.naqibrafiqi.pustaka.data.model.Book

/**
 * Decides the order of the volumes in a series. A volume's number comes from the first rule that
 * gives an answer:
 * 1. the user's own number, set in Edit details;
 * 2. the volume number stored in the EPUB (`group-position` or `calibre:series_index`);
 * 3. a number in the filename, such as `Vol. 04`, `v04`, `#4`, `Volume 4`, or a number at the end.
 *
 * Volumes are sorted by that number. Volumes with the same number, and volumes without one, are
 * sorted by filename with [NaturalOrder], so `10` comes after `9`. Volumes without a number come
 * after the numbered ones.
 */
object VolumeOrder {

    /** `Vol. 4`, `Vol 4`, `Volume 4`, `v04` or `#4`. `v` only counts when it doesn't end a word. */
    private val markedNumber =
        Regex("""(?:\bvol(?:ume)?\.?|(?<![\p{L}\d])v|#)\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

    /** A number at the end of the name. */
    private val trailingNumber = Regex("""(\d+(?:\.\d+)?)$""")

    /** Tags at the end of a name, like `(Digital)` or `[1080p]`, which are skipped. */
    private val trailingTag = Regex("""\s*[(\[{][^)\]}]*[)\]}]\s*$""")

    private val epubExtension = Regex("""\.epub$""", RegexOption.IGNORE_CASE)

    fun volumeNumberOf(book: Book): Double? = volumeNumberOf(
        editedNumber = book.editedDetails.volumeNumber,
        numberInFile = book.sourceDetails.volumeNumber,
        fileName = book.fileName,
    )

    fun volumeNumberOf(editedNumber: Double?, numberInFile: Double?, fileName: String): Double? =
        editedNumber ?: numberInFile ?: numberInFileName(fileName)

    /** Finds the volume number in a filename, or returns null if there isn't one. */
    fun numberInFileName(fileName: String): Double? {
        val name = fileName.replace(epubExtension, "").trim()
        markedNumber.find(name)?.let { return it.groupValues[1].toDouble() }

        var withoutTags = name
        while (true) {
            val stripped = withoutTags.replace(trailingTag, "")
            if (stripped == withoutTags) break
            withoutTags = stripped
        }
        return trailingNumber.find(withoutTags.trimEnd())?.groupValues?.get(1)?.toDouble()
    }

    val comparator: Comparator<Book> =
        compareBy<Book, Double?>(nullsLast(naturalOrder())) { it.volumeNumber }
            .thenBy(NaturalOrder) { it.fileName }
            .thenBy { it.id }

    fun sort(books: List<Book>): List<Book> = books.sortedWith(comparator)
}
