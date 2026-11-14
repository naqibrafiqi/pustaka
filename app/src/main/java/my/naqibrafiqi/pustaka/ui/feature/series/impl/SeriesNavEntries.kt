package my.naqibrafiqi.pustaka.ui.feature.series.impl

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.ui.feature.series.api.BookDetailsRoute
import my.naqibrafiqi.pustaka.ui.feature.series.api.SeriesDetailsRoute
import my.naqibrafiqi.pustaka.ui.feature.series.api.SeriesRoute
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDetailsScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDetailsViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDetailsScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDetailsViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.SeriesScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.SeriesViewModel
import my.naqibrafiqi.pustaka.ui.scan.LibraryScanViewModel
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator

fun EntryProviderScope<NavKey>.seriesEntryBuilder(scanViewModel: LibraryScanViewModel) {
    entry<SeriesRoute> {
        val navigator = LocalAppNavigator.current
        val viewModel: SeriesViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val isRefreshing by scanViewModel.isRefreshing.collectAsStateWithLifecycle()

        SeriesScreen(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = scanViewModel::refresh,
            onRetry = viewModel::retry,
            onOpenSeries = { seriesId -> navigator.add(SeriesDetailsRoute(seriesId)) },
            onOpenBook = { bookId -> navigator.add(BookDetailsRoute(bookId)) },
        )
    }

    entry<SeriesDetailsRoute> { route ->
        val navigator = LocalAppNavigator.current
        val viewModel = hiltViewModel<SeriesDetailsViewModel, SeriesDetailsViewModel.Factory>(
            creationCallback = { factory -> factory.create(route.seriesId) }
        )
        val state by viewModel.state.collectAsStateWithLifecycle()

        SeriesDetailsScreen(
            state = state,
            onNavigateBack = { navigator.removeLastOrNull() },
            onToggleLayout = viewModel::toggleLayout,
            onRetry = viewModel::retry,
            onOpenBook = { bookId -> navigator.add(BookDetailsRoute(bookId)) },
        )
    }

    // Also opened from Library, so it's in that tab's back stack too.
    entry<BookDetailsRoute> { route ->
        val navigator = LocalAppNavigator.current
        val viewModel = hiltViewModel<BookDetailsViewModel, BookDetailsViewModel.Factory>(
            creationCallback = { factory -> factory.create(route.bookId) }
        )
        val state by viewModel.state.collectAsStateWithLifecycle()

        BookDetailsScreen(
            state = state,
            onNavigateBack = { navigator.removeLastOrNull() },
            onRetry = viewModel::retry,
        )
    }
}
