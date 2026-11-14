package my.naqibrafiqi.pustaka.data.model

/**
 * The details of a book that can come from its `content.opf` and can be edited by the user.
 *
 * A [Book] keeps two of these: the details read from the file, and the user's edits. In the
 * edits, a null field means "not edited". Use [overlaidOn] to get the values to show.
 */
data class BookDetails(
    val title: String? = null,
    val creators: List<Creator>? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val identifier: String? = null,
    val language: String? = null,
    val description: String? = null,
    val subjects: List<String>? = null,
    val seriesTitle: String? = null,
    val volumeNumber: Double? = null,
    val coverPath: String? = null,
) {
    /** Fills every field that isn't set here from [source]. */
    fun overlaidOn(source: BookDetails): BookDetails = BookDetails(
        title = title ?: source.title,
        creators = creators ?: source.creators,
        publisher = publisher ?: source.publisher,
        publishedDate = publishedDate ?: source.publishedDate,
        identifier = identifier ?: source.identifier,
        language = language ?: source.language,
        description = description ?: source.description,
        subjects = subjects ?: source.subjects,
        seriesTitle = seriesTitle ?: source.seriesTitle,
        volumeNumber = volumeNumber ?: source.volumeNumber,
        coverPath = coverPath ?: source.coverPath,
    )

    /**
     * Treats this as the values in an edit form and returns the edits to store against [source].
     * See [EditRules] for how blank and unchanged values are handled.
     */
    fun editsAgainst(source: BookDetails): BookDetails = BookDetails(
        title = EditRules.text(title, source.title),
        creators = EditRules.creators(creators, source.creators),
        publisher = EditRules.text(publisher, source.publisher),
        publishedDate = EditRules.text(publishedDate, source.publishedDate),
        identifier = EditRules.text(identifier, source.identifier),
        language = EditRules.text(language, source.language),
        description = EditRules.text(description, source.description),
        subjects = EditRules.list(subjects, source.subjects),
        seriesTitle = EditRules.text(seriesTitle, source.seriesTitle),
        volumeNumber = EditRules.value(volumeNumber, source.volumeNumber),
        coverPath = EditRules.text(coverPath, source.coverPath),
    )

    companion object {
        val EMPTY = BookDetails()
    }
}

val BookDetails.authors: List<String>
    get() = creators.orEmpty().filter { it.isAuthor }.map { it.name }

val BookDetails.illustrators: List<String>
    get() = creatorsWithRole(Creator.ROLE_ILLUSTRATOR)

val BookDetails.translators: List<String>
    get() = creatorsWithRole(Creator.ROLE_TRANSLATOR)

private fun BookDetails.creatorsWithRole(role: String): List<String> =
    creators.orEmpty().filter { it.role == role }.map { it.name }
