package my.naqibrafiqi.pustaka.ui.feature.settings.screen.hidden

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.util.text.UiText
import my.naqibrafiqi.pustaka.util.text.uiText
import javax.inject.Inject

/** A series or book removed from the library, which can be put back. */
data class HiddenItem(
    val id: Long,
    val isSeries: Boolean,
    val name: String,
    /** "Folder: Brunhild", "Standalone book" or "In Brunhild". */
    val description: UiText,
    val coverPath: String?,
)

sealed interface HiddenItemsUiState {

    data object Loading : HiddenItemsUiState

    data class Error(@param:StringRes val message: Int) : HiddenItemsUiState

    data class Ready(val series: List<HiddenItem>, val books: List<HiddenItem>) : HiddenItemsUiState {
        val isEmpty: Boolean get() = series.isEmpty() && books.isEmpty()
    }
}

/** Settings → Hidden items: everything removed with "Remove from library" (decision D7). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HiddenItemsViewModel @Inject constructor(
    private val seriesRepository: SeriesRepository,
    private val books: BookRepository,
) : ViewModel() {

    private val attempt = MutableStateFlow(0)

    val state: StateFlow<HiddenItemsUiState> = attempt
        .flatMapLatest {
            combine(
                seriesRepository.observeHiddenSeries(),
                seriesRepository.observeSeries(),
                books.observeHiddenBooks(),
            ) { hiddenSeries, visibleSeries, hiddenBooks ->
                val seriesNames = (hiddenSeries + visibleSeries).associate { it.id to it.name.value }
                HiddenItemsUiState.Ready(
                    series = hiddenSeries.map { it.toHiddenItem() },
                    books = hiddenBooks.map { it.toHiddenItem(seriesNames[it.seriesId]) },
                ) as HiddenItemsUiState
            }.catch { emit(HiddenItemsUiState.Error(R.string.hidden_error)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HiddenItemsUiState.Loading)

    private val _messages = Channel<UiText>(Channel.BUFFERED)

    /** "“Brunhild” is back in your library." */
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    /** Puts the item back in the library. A restored series brings back its volumes too. */
    fun restore(item: HiddenItem) {
        viewModelScope.launch {
            if (item.isSeries) seriesRepository.setHidden(item.id, false) else books.setHidden(item.id, false)
            _messages.send(uiText(R.string.hidden_restored, item.name))
        }
    }

    fun retry() {
        attempt.value++
    }

    private fun Series.toHiddenItem() = HiddenItem(
        id = id,
        isSeries = true,
        name = name.value,
        description = uiText(R.string.hidden_folder, folderName),
        coverPath = details.coverPath,
    )

    private fun Book.toHiddenItem(seriesName: String?) = HiddenItem(
        id = id,
        isSeries = false,
        name = name.value,
        description = when {
            isStandalone -> uiText(R.string.hidden_standalone_book)
            seriesName != null -> uiText(R.string.search_in_series, seriesName)
            else -> uiText(R.string.hidden_volume_of_series)
        },
        coverPath = details.coverPath,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
