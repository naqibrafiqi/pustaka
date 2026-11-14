package my.naqibrafiqi.pustaka.ui.feature.library.impl

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.ui.feature.library.api.LibraryRoute
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryScreen
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryViewModel
import my.naqibrafiqi.pustaka.ui.feature.reader.rememberReaderLauncher
import my.naqibrafiqi.pustaka.ui.feature.series.api.BookDetailsRoute
import my.naqibrafiqi.pustaka.ui.scan.LibraryScanViewModel
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator

fun EntryProviderScope<NavKey>.libraryEntryBuilder(scanViewModel: LibraryScanViewModel) {
    entry<LibraryRoute> {
        val navigator = LocalAppNavigator.current
        val reader = rememberReaderLauncher()
        val viewModel: LibraryViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val isRefreshing by scanViewModel.isRefreshing.collectAsStateWithLifecycle()

        LibraryScreen(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = scanViewModel::refresh,
            onSelectFilter = viewModel::selectFilter,
            onToggleLayout = viewModel::toggleLayout,
            onRetry = viewModel::retry,
            onOpenBook = { bookId -> navigator.add(BookDetailsRoute(bookId)) },
            onResumeBook = { bookId -> reader.open(bookId, fromStart = false) },
        )
    }
}
