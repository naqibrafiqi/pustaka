package my.naqibrafiqi.pustaka.ui.model

import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.util.text.UiText

/** A volume in the series card's preview strip. */
data class VolumePreviewItem(
    val bookId: Long,
    val label: UiText,
    val coverPath: String?,
    val state: BookState,
)

/** A series as shown on its card in the Series tab. */
data class SeriesItem(
    val id: Long,
    val name: String,
    val author: String?,
    val illustrator: String?,
    /** The main genre first, then the tags. */
    val genres: List<String>,
    val coverPath: String?,
    val volumeCount: Int,
    val finishedCount: Int,
    /** Finished volumes out of all volumes, from 0 to 100. */
    val percent: Int,
    /** "Reading: Volume 2", "Up next: Volume 3", ... */
    val currentLabel: UiText,
    /** The volume "Continue reading" goes to, or null when there's none to read. */
    val continueBookId: Long?,
    val preview: List<VolumePreviewItem>,
) {
    /** "Author, Illustrator", or whichever is known. */
    val credits: String? get() = listOfNotNull(author, illustrator).joinToString(", ").ifEmpty { null }
}

/** [volumes] must be the series' volumes in reading order. */
fun Series.toItem(volumes: List<Book>): SeriesItem {
    val progress = SeriesProgress(volumes)
    val current = progress.current
    return SeriesItem(
        id = id,
        name = name.value,
        author = details.author,
        illustrator = details.illustrator,
        genres = genresOf(this),
        // The series' own cover (D25), so a cover picked in Edit series shows here too.
        coverPath = details.coverPath,
        volumeCount = progress.total,
        finishedCount = progress.finished,
        percent = progress.percent,
        currentLabel = progress.currentLabel,
        continueBookId = current?.id,
        preview = progress.preview().map { book ->
            VolumePreviewItem(
                bookId = book.id,
                label = progress.labelOf(book),
                coverPath = book.details.coverPath,
                state = BookState.of(book),
            )
        },
    )
}

fun genresOf(series: Series): List<String> =
    (listOfNotNull(series.details.mainGenre) + series.details.tags.orEmpty()).distinct()
