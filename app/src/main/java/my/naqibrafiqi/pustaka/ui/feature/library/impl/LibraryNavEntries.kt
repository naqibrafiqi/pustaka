package my.naqibrafiqi.pustaka.ui.feature.library.impl

import androidx.navigation3.runtime.EntryProviderScope
import my.naqibrafiqi.pustaka.PustakaRoute
import my.naqibrafiqi.pustaka.ui.feature.library.api.LibraryRoute
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryScreen

fun EntryProviderScope<PustakaRoute>.libraryEntryBuilder() {
    entry<LibraryRoute> {
        LibraryScreen()
    }
}
