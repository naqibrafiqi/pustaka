package my.naqibrafiqi.pustaka.ui.feature.library.impl

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.ui.feature.library.api.LibraryRoute
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryScreen
import my.naqibrafiqi.pustaka.ui.feature.series.api.BookDetailsRoute
import my.naqibrafiqi.pustaka.ui.scan.LibraryScanViewModel
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator

fun EntryProviderScope<NavKey>.libraryEntryBuilder() {
    entry<LibraryRoute> {
        val navigator = LocalAppNavigator.current
        val scanViewModel: LibraryScanViewModel = hiltViewModel()
        val isRefreshing by scanViewModel.isRefreshing.collectAsStateWithLifecycle()

        LibraryScreen(
            onNavigateToBookDetail = { bookId ->
                navigator.add(BookDetailsRoute(bookId))
            },
            isRefreshing = isRefreshing,
            onRefresh = scanViewModel::refresh,
        )
    }
}
