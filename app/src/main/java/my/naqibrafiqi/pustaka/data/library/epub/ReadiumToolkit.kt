package my.naqibrafiqi.pustaka.data.library.epub

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Why an EPUB couldn't be opened. */
enum class OpenFailure {
    /** The file isn't there, or can't be read. */
    NOT_READABLE,

    /** The file isn't an EPUB, or is damaged. */
    NOT_EPUB,

    /** The book is locked with DRM, which Pustaka can't read. */
    PROTECTED,
}

sealed interface OpenResult {
    /** The caller owns [publication] and must close it. */
    data class Opened(val publication: Publication) : OpenResult

    data class Failed(val reason: OpenFailure) : OpenResult
}

/**
 * Opens EPUB files with the Readium Kotlin Toolkit. The scanner uses it to read book details and
 * the reader uses it to show the book, so both see the same publication.
 */
@Singleton
class ReadiumToolkit @Inject constructor(@ApplicationContext context: Context) {

    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context = context,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null,
        ),
    )

    /**
     * @param allowProtected opens DRM-locked books too. Their details can still be read, but not
     *   their contents.
     */
    suspend fun open(file: File, allowProtected: Boolean = false): OpenResult = withContext(Dispatchers.IO) {
        if (!file.canRead()) return@withContext OpenResult.Failed(OpenFailure.NOT_READABLE)
        val asset = assetRetriever.retrieve(file)
            .getOrElse { return@withContext OpenResult.Failed(OpenFailure.NOT_EPUB) }
        val publication = publicationOpener.open(asset, allowUserInteraction = false)
            .getOrElse {
                asset.close()
                return@withContext OpenResult.Failed(OpenFailure.NOT_EPUB)
            }
        val failure = when {
            !publication.conformsTo(Publication.Profile.EPUB) -> OpenFailure.NOT_EPUB
            !allowProtected && publication.isRestricted -> OpenFailure.PROTECTED
            else -> null
        }
        if (failure != null) {
            publication.close()
            OpenResult.Failed(failure)
        } else {
            OpenResult.Opened(publication)
        }
    }
}
