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
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDetailsScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.SeriesScreen
import my.naqibrafiqi.pustaka.ui.scan.LibraryScanViewModel
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator

fun EntryProviderScope<NavKey>.seriesEntryBuilder() {
    entry<SeriesRoute> {
        val navigator = LocalAppNavigator.current
        val scanViewModel: LibraryScanViewModel = hiltViewModel()
        val isRefreshing by scanViewModel.isRefreshing.collectAsStateWithLifecycle()

        SeriesScreen(
            onNavigateToSeriesDetails = { seriesId ->
                navigator.add(SeriesDetailsRoute(seriesId))
            },
            isRefreshing = isRefreshing,
            onRefresh = scanViewModel::refresh,
        )
    }

    entry<SeriesDetailsRoute> {
        val navigator = LocalAppNavigator.current

        SeriesDetailsScreen(
            onNavigateBack = {
                navigator.removeLastOrNull()
            }
        )
    }

    entry<BookDetailsRoute> {
        val navigator = LocalAppNavigator.current

        BookDetailsScreen(
            onNavigateBack = {
                navigator.removeLastOrNull()
            }
        )
    }
}
