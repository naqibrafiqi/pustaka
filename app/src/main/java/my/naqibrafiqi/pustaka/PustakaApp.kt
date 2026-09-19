package my.naqibrafiqi.pustaka

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import my.naqibrafiqi.pustaka.ui.components.BottomBar
import my.naqibrafiqi.pustaka.ui.components.BottomBarNavItems
import my.naqibrafiqi.pustaka.ui.feature.highlights.api.HighlightsRoute
import my.naqibrafiqi.pustaka.ui.feature.highlights.impl.highlightsEntryBuilder
import my.naqibrafiqi.pustaka.ui.feature.library.api.LibraryRoute
import my.naqibrafiqi.pustaka.ui.feature.library.impl.libraryEntryBuilder
import my.naqibrafiqi.pustaka.ui.feature.series.api.SeriesRoute
import my.naqibrafiqi.pustaka.ui.feature.series.impl.seriesEntryBuilder
import my.naqibrafiqi.pustaka.ui.feature.settings.api.SettingsRoute
import my.naqibrafiqi.pustaka.ui.feature.settings.impl.settingsEntryBuilder
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator

@Composable
fun PustakaApp() {
    val libraryBackStack = rememberNavBackStack(LibraryRoute)
    val seriesBackStack = rememberNavBackStack(SeriesRoute)
    val highlightsBackStack = rememberNavBackStack(HighlightsRoute)
    val settingsBackStack = rememberNavBackStack(SettingsRoute)

    val tabs = BottomBarNavItems

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val selectedTabRoute = tabs[selectedTabIndex].route

    val currentBackStack = when (selectedTabRoute) {
        LibraryRoute -> libraryBackStack
        SeriesRoute -> seriesBackStack
        HighlightsRoute -> highlightsBackStack
        SettingsRoute -> settingsBackStack
        else -> libraryBackStack
    }

    val isMainScreen = currentBackStack.size == 1

    BackHandler(enabled = selectedTabRoute != LibraryRoute && currentBackStack.size == 1) {
        selectedTabIndex = 0
    }

    CompositionLocalProvider(LocalAppNavigator provides currentBackStack) {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = isMainScreen,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    BottomBar(
                        currentItem = tabs.find { it.route == selectedTabRoute },
                        items = tabs,
                        onClick = { index, _ ->
                            selectedTabIndex = index
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavDisplay(
                backStack = currentBackStack,
                modifier = Modifier.padding(innerPadding),
                onBack = {
                    currentBackStack.removeLastOrNull()
                },
                entryProvider = entryProvider {
                    libraryEntryBuilder()
                    seriesEntryBuilder()
                    highlightsEntryBuilder()
                    settingsEntryBuilder()
                }
            )
        }
    }
}
