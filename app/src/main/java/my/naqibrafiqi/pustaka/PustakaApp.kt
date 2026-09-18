package my.naqibrafiqi.pustaka

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import my.naqibrafiqi.pustaka.ui.feature.library.api.LibraryRoute
import my.naqibrafiqi.pustaka.ui.feature.library.impl.libraryEntryBuilder

@Composable
fun PustakaApp() {
    val backStack = rememberNavBackStack(LibraryRoute)

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.statusBarsPadding(),
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            libraryEntryBuilder()
        }
    )
}
