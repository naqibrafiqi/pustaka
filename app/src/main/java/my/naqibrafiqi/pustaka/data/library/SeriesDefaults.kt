package my.naqibrafiqi.pustaka.data.library

import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import my.naqibrafiqi.pustaka.data.model.authors
import my.naqibrafiqi.pustaka.data.model.illustrators

/**
 * A series' default details come from its volumes in volume order (see [VolumeOrder]), using the
 * details read from their files. Hidden volumes are left out.
 *
 * From the first volume:
 * - author and illustrator: its authors and illustrators, joined with commas;
 * - synopsis: its description;
 * - main genre: its first subject, and tags: the other subjects.
 *
 * From the last volume (D25): the cover. A volume without a cover is skipped, and one whose file is
 * missing is only used when no other volume has a cover. Edit series can pick another volume's
 * cover instead ([coversOf] lists them).
 */
object SeriesDefaults {

    fun fromVolumes(volumes: List<Book>): SeriesDetails {
        val ordered = VolumeOrder.sort(volumes.filterNot { it.isHidden })
        val first = ordered.firstOrNull() ?: return SeriesDetails.EMPTY
        val details = first.sourceDetails
        val subjects = details.subjects.orEmpty()
        val withCover = ordered.filter { it.sourceDetails.coverPath != null }
        val coverBook = withCover.lastOrNull { !it.isMissing } ?: withCover.lastOrNull()
        return SeriesDetails(
            author = details.authors.joinToString(", ").ifEmpty { null },
            illustrator = details.illustrators.joinToString(", ").ifEmpty { null },
            synopsis = details.description,
            mainGenre = subjects.firstOrNull(),
            tags = subjects.drop(1).ifEmpty { null },
            coverPath = coverBook?.sourceDetails?.coverPath,
        )
    }

    /** The covers a series can use: those of its volumes that aren't hidden. */
    fun coversOf(volumes: List<Book>): Set<String> =
        volumes.filterNot { it.isHidden }.mapNotNullTo(LinkedHashSet()) { it.details.coverPath }
}
