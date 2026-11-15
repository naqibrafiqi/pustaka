package my.naqibrafiqi.pustaka.ui.feature.series.screen.edit

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.library.VolumeOrder
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.Creator
import my.naqibrafiqi.pustaka.data.model.authors
import my.naqibrafiqi.pustaka.data.model.illustrators
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.ui.model.formatVolumeNumber

/**
 * Edit details for a book. Its name defaults to the file name, and its other details to those in
 * its `content.opf`. "Use details from file" also takes the title stored in the file as the name.
 */
@HiltViewModel(assistedFactory = EditBookViewModel.Factory::class)
class EditBookViewModel @AssistedInject constructor(
    @Assisted private val bookId: Long,
    private val books: BookRepository,
) : EditDetailsViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(bookId: Long): EditBookViewModel
    }

    private lateinit var book: Book

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val found = books.getBook(bookId)
        if (found == null) {
            mutableState.value = EditUiState.NotFound
            return
        }
        book = found
        val shown = found.details
        val source = found.sourceDetails

        val fields = buildList {
            add(EditField(NAME, R.string.edit_field_name, found.name.value, found.name.default))
            if (!found.isStandalone) {
                add(
                    EditField(
                        VOLUME, R.string.edit_field_volume,
                        found.volumeNumber?.let(::formatVolumeNumber).orEmpty(),
                        defaultVolume()?.let(::formatVolumeNumber).orEmpty(),
                        isNumber = true,
                        hint = R.string.edit_hint_volume,
                    )
                )
            }
            add(EditField(AUTHORS, R.string.edit_field_authors, joinList(shown.authors), joinList(source.authors), hint = NAMES_HINT))
            add(
                EditField(
                    ILLUSTRATORS, R.string.edit_field_illustrators, joinList(shown.illustrators), joinList(source.illustrators),
                    hint = NAMES_HINT,
                )
            )
            add(EditField(DESCRIPTION, R.string.edit_field_description, shown.description.orEmpty(), source.description.orEmpty(), multiline = true))
            add(
                EditField(
                    SUBJECTS, R.string.edit_field_genres, joinList(shown.subjects), joinList(source.subjects),
                    hint = R.string.edit_hint_genres,
                )
            )
            add(EditField(PUBLISHER, R.string.book_info_publisher, shown.publisher.orEmpty(), source.publisher.orEmpty()))
            add(EditField(PUBLISHED, R.string.book_info_published, shown.publishedDate.orEmpty(), source.publishedDate.orEmpty()))
            add(EditField(LANGUAGE, R.string.book_info_language, shown.language.orEmpty(), source.language.orEmpty()))
            add(EditField(IDENTIFIER, R.string.book_info_identifier, shown.identifier.orEmpty(), source.identifier.orEmpty()))
        }
        mutableState.value = EditUiState.Ready(
            subtitle = found.name.value,
            fields = fields,
            canUseFileDetails = source != BookDetails.EMPTY,
        )
    }

    /** The volume number without the user's edit: from the file, or from the file name. */
    private fun defaultVolume(): Double? =
        VolumeOrder.volumeNumberOf(null, book.sourceDetails.volumeNumber, book.fileName)

    override fun fileValues(): Map<String, String> {
        val source = book.sourceDetails
        return mapOf(
            NAME to (source.title ?: book.name.default),
            VOLUME to defaultVolume()?.let(::formatVolumeNumber).orEmpty(),
            AUTHORS to joinList(source.authors),
            ILLUSTRATORS to joinList(source.illustrators),
            DESCRIPTION to source.description.orEmpty(),
            SUBJECTS to joinList(source.subjects),
            PUBLISHER to source.publisher.orEmpty(),
            PUBLISHED to source.publishedDate.orEmpty(),
            LANGUAGE to source.language.orEmpty(),
            IDENTIFIER to source.identifier.orEmpty(),
        )
    }

    override fun validate(form: EditUiState.Ready): Map<String, Int> {
        val volume = form.fields.firstOrNull { it.key == VOLUME }?.value?.trim().orEmpty()
        return if (volume.isNotEmpty() && volume.toDoubleOrNull()?.takeIf { it >= 0 } == null) {
            mapOf(VOLUME to R.string.edit_error_volume)
        } else {
            emptyMap()
        }
    }

    override suspend fun persist(form: EditUiState.Ready) {
        val source = book.sourceDetails
        val current = book.details
        books.updateEdits(
            bookId,
            name = form.value(NAME),
            details = BookDetails(
                // Not in the form, so the values shown now are kept.
                title = current.title,
                seriesTitle = current.seriesTitle,
                coverPath = current.coverPath,
                creators = creators(form, source, current),
                publisher = form.value(PUBLISHER),
                publishedDate = form.value(PUBLISHED),
                identifier = form.value(IDENTIFIER),
                language = form.value(LANGUAGE),
                description = form.value(DESCRIPTION),
                subjects = splitList(form.value(SUBJECTS)),
                volumeNumber = volumeNumber(form),
            ),
        )
    }

    /**
     * The file's own creators when authors and illustrators are left as they are in the file.
     * Otherwise the names typed in, plus anyone credited with another role, such as a translator.
     */
    private fun creators(form: EditUiState.Ready, source: BookDetails, current: BookDetails): List<Creator>? {
        val authors = splitList(form.value(AUTHORS))
        val illustrators = splitList(form.value(ILLUSTRATORS))
        if (authors == source.authors && illustrators == source.illustrators) return source.creators
        val others = current.creators.orEmpty().filterNot { it.isAuthor || it.role == Creator.ROLE_ILLUSTRATOR }
        return authors.map { Creator(it, Creator.ROLE_AUTHOR) } +
            illustrators.map { Creator(it, Creator.ROLE_ILLUSTRATOR) } +
            others
    }

    /** A number that matches the default isn't kept as an edit. */
    private fun volumeNumber(form: EditUiState.Ready): Double? {
        if (book.isStandalone) return book.editedDetails.volumeNumber ?: book.sourceDetails.volumeNumber
        val typed = form.value(VOLUME).trim().toDoubleOrNull()
        return if (typed == null || typed == defaultVolume()) book.sourceDetails.volumeNumber else typed
    }

    companion object {
        const val NAME = "name"
        const val VOLUME = "volume"
        const val AUTHORS = "authors"
        const val ILLUSTRATORS = "illustrators"
        const val DESCRIPTION = "description"
        const val SUBJECTS = "subjects"
        const val PUBLISHER = "publisher"
        const val PUBLISHED = "published"
        const val LANGUAGE = "language"
        const val IDENTIFIER = "identifier"

        private val NAMES_HINT = R.string.edit_hint_names
    }
}
