package my.naqibrafiqi.pustaka.ui.feature.search.impl

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.ui.feature.search.api.SearchRoute
import my.naqibrafiqi.pustaka.ui.feature.search.screen.SearchScreen
import my.naqibrafiqi.pustaka.ui.feature.search.screen.SearchViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.api.BookDetailsRoute
import my.naqibrafiqi.pustaka.ui.feature.series.api.SeriesDetailsRoute
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator

fun EntryProviderScope<NavKey>.searchEntryBuilder() {
    entry<SearchRoute> {
        val navigator = LocalAppNavigator.current
        val viewModel: SearchViewModel = hiltViewModel()
        val query by viewModel.query.collectAsStateWithLifecycle()
        val state by viewModel.state.collectAsStateWithLifecycle()

        SearchScreen(
            query = query,
            state = state,
            onQueryChange = viewModel::changeQuery,
            onOpenSeries = { seriesId -> navigator.add(SeriesDetailsRoute(seriesId)) },
            onOpenBook = { bookId -> navigator.add(BookDetailsRoute(bookId)) },
            onNavigateBack = { navigator.removeLastOrNull() },
        )
    }
}
