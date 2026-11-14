package my.naqibrafiqi.pustaka.data.library

import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import my.naqibrafiqi.pustaka.data.model.authors
import my.naqibrafiqi.pustaka.data.model.illustrators

/**
 * A series' default details come from its first volume (see [VolumeOrder]), using the details read
 * from that volume's file:
 * - author and illustrator: its authors and illustrators, joined with commas;
 * - synopsis: its description;
 * - main genre: its first subject, and tags: the other subjects;
 * - cover: its cover.
 */
object SeriesDefaults {

    fun fromVolumes(volumes: List<Book>): SeriesDetails {
        val first = VolumeOrder.sort(volumes.filterNot { it.isHidden }).firstOrNull()
            ?: return SeriesDetails.EMPTY
        val details = first.sourceDetails
        val subjects = details.subjects.orEmpty()
        return SeriesDetails(
            author = details.authors.joinToString(", ").ifEmpty { null },
            illustrator = details.illustrators.joinToString(", ").ifEmpty { null },
            synopsis = details.description,
            mainGenre = subjects.firstOrNull(),
            tags = subjects.drop(1).ifEmpty { null },
            coverPath = details.coverPath,
        )
    }
}
