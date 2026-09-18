package my.naqibrafiqi.pustaka.ui.feature.library.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.ui.feature.library.api.LibraryRoute
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryScreen

fun EntryProviderScope<NavKey>.libraryEntryBuilder() {
    entry<LibraryRoute> {
        LibraryScreen()
    }
}
