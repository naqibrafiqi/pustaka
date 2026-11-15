package my.naqibrafiqi.pustaka.ui.feature.search.screen

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import javax.inject.Inject

sealed interface SearchUiState {

    /** Nothing typed yet. */
    data object Idle : SearchUiState

    data class Results(val query: String, val results: SearchResults) : SearchUiState

    data class Error(@param:StringRes val message: Int) : SearchUiState
}

/** The search screen opened from Library and Series. Hidden series and books aren't searched. */
@HiltViewModel
class SearchViewModel @Inject constructor(
    seriesRepository: SeriesRepository,
    books: BookRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val _query = savedState.getMutableStateFlow(KEY_QUERY, "")
    val query: StateFlow<String> = _query

    val state: StateFlow<SearchUiState> = combine(
        _query,
        seriesRepository.observeSeries(),
        books.observeLibraryBooks(),
    ) { query, series, library ->
        if (query.isBlank()) {
            SearchUiState.Idle
        } else {
            SearchUiState.Results(query.trim(), LibrarySearch.search(query, series, library))
        }
    }
        .catch<SearchUiState> { emit(SearchUiState.Error(R.string.search_error)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchUiState.Idle)

    fun changeQuery(query: String) {
        _query.value = query
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
