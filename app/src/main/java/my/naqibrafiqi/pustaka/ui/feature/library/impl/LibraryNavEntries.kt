package my.naqibrafiqi.pustaka.ui.feature.library.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.ui.feature.library.api.LibraryRoute
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryScreen
import my.naqibrafiqi.pustaka.ui.feature.series.api.BookDetailsRoute
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator

fun EntryProviderScope<NavKey>.libraryEntryBuilder() {
    entry<LibraryRoute> {
        val navigator = LocalAppNavigator.current

        LibraryScreen(
            onNavigateToBookDetail = { bookId ->
                navigator.add(BookDetailsRoute(bookId))
            }
        )
    }
}
