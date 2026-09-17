package my.naqibrafiqi.pustaka

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import my.naqibrafiqi.pustaka.ui.feature.library.api.LibraryRoute
import my.naqibrafiqi.pustaka.ui.feature.library.impl.libraryEntryBuilder
import my.naqibrafiqi.pustaka.util.navigation.rememberPustakaNavBackStack

@Composable
fun PustakaApp() {
    val backStack = rememberPustakaNavBackStack(LibraryRoute)

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.statusBarsPadding(),
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            libraryEntryBuilder()
        }
    )
}
