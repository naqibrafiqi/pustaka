package my.naqibrafiqi.pustaka.data.model

/**
 * The details of a series other than its name. By default they're filled from the series' first
 * volume, except the cover, which comes from its last (see SeriesDefaults). Each one can be edited
 * by the user, and the cover can be any volume's cover.
 *
 * A [Series] keeps two of these: the defaults and the user's edits. In the edits, a null field
 * means "not edited". Use [overlaidOn] to get the values to show.
 */
data class SeriesDetails(
    val author: String? = null,
    val illustrator: String? = null,
    val synopsis: String? = null,
    val mainGenre: String? = null,
    val tags: List<String>? = null,
    val coverPath: String? = null,
) {
    /** Fills every field that isn't set here from [source]. */
    fun overlaidOn(source: SeriesDetails): SeriesDetails = SeriesDetails(
        author = author ?: source.author,
        illustrator = illustrator ?: source.illustrator,
        synopsis = synopsis ?: source.synopsis,
        mainGenre = mainGenre ?: source.mainGenre,
        tags = tags ?: source.tags,
        coverPath = coverPath ?: source.coverPath,
    )

    /**
     * Treats this as the values in an edit form and returns the edits to store against [source].
     * See [EditRules] for how blank and unchanged values are handled.
     */
    fun editsAgainst(source: SeriesDetails): SeriesDetails = SeriesDetails(
        author = EditRules.text(author, source.author),
        illustrator = EditRules.text(illustrator, source.illustrator),
        synopsis = EditRules.text(synopsis, source.synopsis),
        mainGenre = EditRules.text(mainGenre, source.mainGenre),
        tags = EditRules.list(tags, source.tags),
        coverPath = EditRules.text(coverPath, source.coverPath),
    )

    companion object {
        val EMPTY = SeriesDetails()
    }
}
