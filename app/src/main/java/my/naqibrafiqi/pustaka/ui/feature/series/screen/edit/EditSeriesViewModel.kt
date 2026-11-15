package my.naqibrafiqi.pustaka.ui.feature.series.screen.edit

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository

/**
 * Edit details for a series. Its name defaults to the folder name, and its other details to those
 * of its first volume. "Use details from file" also takes the series title stored in that volume.
 */
@HiltViewModel(assistedFactory = EditSeriesViewModel.Factory::class)
class EditSeriesViewModel @AssistedInject constructor(
    @Assisted private val seriesId: Long,
    private val seriesRepository: SeriesRepository,
    private val books: BookRepository,
) : EditDetailsViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(seriesId: Long): EditSeriesViewModel
    }

    private lateinit var series: Series
    private var titleInFile: String? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val found = seriesRepository.getSeries(seriesId)
        if (found == null) {
            mutableState.value = EditUiState.NotFound
            return
        }
        series = found
        titleInFile = books.observeVolumes(seriesId).first().firstNotNullOfOrNull { it.sourceDetails.seriesTitle }

        val shown = found.details
        val source = found.sourceDetails
        mutableState.value = EditUiState.Ready(
            subtitle = found.name.value,
            fields = listOf(
                EditField(NAME, R.string.edit_field_name, found.name.value, found.folderName),
                EditField(AUTHOR, R.string.details_author, shown.author.orEmpty(), source.author.orEmpty()),
                EditField(ILLUSTRATOR, R.string.details_illustrator, shown.illustrator.orEmpty(), source.illustrator.orEmpty()),
                EditField(MAIN_GENRE, R.string.edit_field_main_genre, shown.mainGenre.orEmpty(), source.mainGenre.orEmpty()),
                EditField(
                    TAGS, R.string.edit_field_tags, joinList(shown.tags), joinList(source.tags),
                    hint = R.string.edit_hint_tags,
                ),
                EditField(SYNOPSIS, R.string.edit_field_synopsis, shown.synopsis.orEmpty(), source.synopsis.orEmpty(), multiline = true),
            ),
            canUseFileDetails = titleInFile != null || source != SeriesDetails.EMPTY,
        )
    }

    override fun fileValues(): Map<String, String> {
        val source = series.sourceDetails
        return mapOf(
            NAME to (titleInFile ?: series.folderName),
            AUTHOR to source.author.orEmpty(),
            ILLUSTRATOR to source.illustrator.orEmpty(),
            MAIN_GENRE to source.mainGenre.orEmpty(),
            TAGS to joinList(source.tags),
            SYNOPSIS to source.synopsis.orEmpty(),
        )
    }

    override suspend fun persist(form: EditUiState.Ready) {
        seriesRepository.updateEdits(
            seriesId,
            name = form.value(NAME),
            details = SeriesDetails(
                author = form.value(AUTHOR),
                illustrator = form.value(ILLUSTRATOR),
                synopsis = form.value(SYNOPSIS),
                mainGenre = form.value(MAIN_GENRE),
                tags = splitList(form.value(TAGS)),
                // The cover can't be changed here, so whatever is shown stays.
                coverPath = series.details.coverPath,
            ),
        )
    }

    companion object {
        const val NAME = "name"
        const val AUTHOR = "author"
        const val ILLUSTRATOR = "illustrator"
        const val MAIN_GENRE = "mainGenre"
        const val TAGS = "tags"
        const val SYNOPSIS = "synopsis"
    }
}
