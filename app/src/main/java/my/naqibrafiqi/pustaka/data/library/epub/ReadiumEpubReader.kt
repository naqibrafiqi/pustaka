package my.naqibrafiqi.pustaka.data.library.epub

import android.util.Size
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.Creator
import org.readium.r2.shared.publication.Contributor
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.services.coverFitting
import org.readium.r2.shared.publication.services.positions
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime

/** Reads EPUB details with the Readium Kotlin Toolkit, which is also the reader engine. */
@Singleton
class ReadiumEpubReader @Inject constructor(
    private val toolkit: ReadiumToolkit,
    private val coverStore: CoverStore,
) : EpubReader {

    override suspend fun read(file: File, fingerprint: String): EpubMetadata? =
        withContext(Dispatchers.IO) {
            val opened = toolkit.open(file, allowProtected = true) as? OpenResult.Opened
                ?: return@withContext null
            val publication = opened.publication
            try {
                val metadata = publication.metadata
                val creators = OpfCreators.read(file) ?: creatorsFrom(metadata)
                val coverPath = runCatching { publication.coverFitting(MAX_COVER_SIZE) }
                    .getOrNull()
                    ?.let { coverStore.save(fingerprint, it) }
                val totalPositions = runCatching { publication.positions().size }
                    .getOrNull()
                    ?.takeIf { it > 0 }
                EpubMetadata(
                    details = metadata.toBookDetails(creators, coverPath),
                    totalPositions = totalPositions,
                )
            } finally {
                publication.close()
            }
        }

    // Readium's Instant is built on kotlin.time.Instant, which is still marked experimental.
    @OptIn(ExperimentalTime::class)
    private fun Metadata.toBookDetails(creators: List<Creator>, coverPath: String?): BookDetails {
        val series = belongsToSeries.firstOrNull()
        return BookDetails(
            title = title.clean(),
            creators = creators.ifEmpty { null },
            publisher = publishers.joinToString(", ") { it.name.trim() }.clean(),
            // Readium turns `dc:date` into a full timestamp. Only the date part is meaningful.
            publishedDate = published?.toString()?.take(DATE_LENGTH),
            identifier = identifier.clean(),
            language = languages.firstOrNull().clean(),
            description = description?.let(::plainText).clean(),
            subjects = subjects.mapNotNull { it.name.clean() }.distinct().ifEmpty { null },
            seriesTitle = series?.name.clean(),
            volumeNumber = series?.position,
            coverPath = coverPath,
        )
    }

    /** Used when `content.opf` can't be read directly. Readium's roles are less precise. */
    private fun creatorsFrom(metadata: Metadata): List<Creator> = buildList {
        fun add(contributors: List<Contributor>, role: String?) =
            contributors.forEach { add(Creator(it.name.trim(), role ?: it.roles.firstOrNull())) }
        add(metadata.authors, null)
        add(metadata.illustrators, Creator.ROLE_ILLUSTRATOR)
        add(metadata.translators, Creator.ROLE_TRANSLATOR)
        add(metadata.editors, Creator.ROLE_EDITOR)
        add(metadata.artists, "art")
        add(metadata.colorists, "clr")
        add(metadata.narrators, "nrt")
        add(metadata.contributors, OpfCreators.ROLE_CONTRIBUTOR)
    }.filter { it.name.isNotEmpty() }.distinct()

    /** Descriptions are often HTML, for example in books from Calibre. */
    private fun plainText(text: String): String =
        if ('<' in text || '&' in text) {
            HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
        } else {
            text
        }

    private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }

    private companion object {
        val MAX_COVER_SIZE = Size(1000, 1500)
        const val DATE_LENGTH = 10
    }
}
