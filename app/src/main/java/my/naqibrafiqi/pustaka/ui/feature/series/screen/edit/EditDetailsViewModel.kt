package my.naqibrafiqi.pustaka.ui.feature.series.screen.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One field in an Edit details form.
 *
 * @property default the value the field resets to: from the file, or from the folder or file name.
 * @property error shown under the field when its value can't be saved.
 */
data class EditField(
    val key: String,
    val label: String,
    val value: String,
    val default: String,
    val multiline: Boolean = false,
    val isNumber: Boolean = false,
    /** For example "Separate names with commas". */
    val hint: String? = null,
    val error: String? = null,
) {
    /** Whether the field differs from its default, so it shows a reset button. */
    val isEdited: Boolean get() = value.trim() != default.trim()
}

sealed interface EditUiState {

    data object Loading : EditUiState

    /** The series or book was deleted while the form was opening. */
    data object NotFound : EditUiState

    data class Ready(
        /** The name of the series or book, under the screen title. */
        val subtitle: String,
        val fields: List<EditField>,
        /** Whether there are details from a file for "Use details from file" to fill in. */
        val canUseFileDetails: Boolean,
        /** True once anything has been typed, so leaving asks first. */
        val hasChanges: Boolean = false,
        val isSaving: Boolean = false,
        val error: String? = null,
    ) : EditUiState {
        fun value(key: String): String = fields.first { it.key == key }.value
    }
}

/**
 * The shared part of the Edit details screens for series and books. Edits are saved in the app's
 * database and never written to the files. A field that's blank, or matches its default, isn't kept
 * as an edit, so it goes back to following the file.
 */
abstract class EditDetailsViewModel : ViewModel() {

    protected val mutableState = MutableStateFlow<EditUiState>(EditUiState.Loading)
    val state: StateFlow<EditUiState> = mutableState.asStateFlow()

    private val _saved = Channel<Unit>(Channel.CONFLATED)

    /** Emits once the edits are saved, so the screen can close. */
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    /** The values "Use details from file" fills in, by field key. */
    protected abstract fun fileValues(): Map<String, String>

    /** Checks the form. Returns an error for each field key that can't be saved. */
    protected open fun validate(form: EditUiState.Ready): Map<String, String> = emptyMap()

    /** Saves the form's values. */
    protected abstract suspend fun persist(form: EditUiState.Ready)

    fun change(key: String, value: String) = updateFields { field ->
        if (field.key == key) field.copy(value = value, error = null) else field
    }

    fun reset(key: String) = updateFields { field ->
        if (field.key == key) field.copy(value = field.default, error = null) else field
    }

    fun useDetailsFromFile() {
        // The file's details are only known once the form has loaded.
        if (mutableState.value !is EditUiState.Ready) return
        val values = fileValues()
        updateFields { field -> values[field.key]?.let { field.copy(value = it, error = null) } ?: field }
    }

    fun save() {
        val form = mutableState.value as? EditUiState.Ready ?: return
        if (form.isSaving) return
        val errors = validate(form)
        if (errors.isNotEmpty()) {
            mutableState.value = form.copy(fields = form.fields.map { it.copy(error = errors[it.key]) })
            return
        }
        mutableState.value = form.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                persist(form)
                _saved.send(Unit)
            } catch (e: Exception) {
                mutableState.update { (it as? EditUiState.Ready)?.copy(isSaving = false, error = "Couldn't save the changes.") ?: it }
            }
        }
    }

    private fun updateFields(change: (EditField) -> EditField) {
        mutableState.update { state ->
            if (state !is EditUiState.Ready) return@update state
            val fields = state.fields.map(change)
            state.copy(fields = fields, hasChanges = state.hasChanges || fields != state.fields)
        }
    }

    protected companion object {
        /** "a, b, c" → [a, b, c], leaving out blanks. */
        fun splitList(text: String): List<String> = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }

        fun joinList(items: List<String>?): String = items.orEmpty().joinToString(", ")
    }
}
