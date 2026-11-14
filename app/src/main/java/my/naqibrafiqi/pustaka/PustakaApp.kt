package my.naqibrafiqi.pustaka

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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
import my.naqibrafiqi.pustaka.ui.scan.LibraryScanViewModel
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator

@Composable
fun PustakaApp() {
    // Shared by the whole app, so the screens' pull-to-refresh and this message use the same one.
    val scanViewModel: LibraryScanViewModel = hiltViewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(scanViewModel) {
        scanViewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val libraryBackStack = rememberNavBackStack(LibraryRoute)
    val seriesBackStack = rememberNavBackStack(SeriesRoute)
    val highlightsBackStack = rememberNavBackStack(HighlightsRoute)
    val settingsBackStack = rememberNavBackStack(SettingsRoute)

    val tabs = BottomBarNavItems

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val selectedTabRoute = tabs[selectedTabIndex].route

    val entryProvider = entryProvider<NavKey> {
        libraryEntryBuilder(scanViewModel)
        seriesEntryBuilder(scanViewModel)
        highlightsEntryBuilder()
        settingsEntryBuilder(scanViewModel)
    }

    // Each tab keeps its screens' state and ViewModels while another tab is showing. They're only
    // cleared when a screen is removed from its tab's back stack.
    val libraryEntries = rememberTabEntries("library", libraryBackStack, entryProvider)
    val seriesEntries = rememberTabEntries("series", seriesBackStack, entryProvider)
    val highlightsEntries = rememberTabEntries("highlights", highlightsBackStack, entryProvider)
    val settingsEntries = rememberTabEntries("settings", settingsBackStack, entryProvider)

    val currentEntries = when (selectedTabRoute) {
        SeriesRoute -> seriesEntries
        HighlightsRoute -> highlightsEntries
        SettingsRoute -> settingsEntries
        else -> libraryEntries
    }

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
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                entries = currentEntries,
                modifier = Modifier.padding(innerPadding),
                onBack = {
                    currentBackStack.removeLastOrNull()
                },
            )
        }
    }
}

@Composable
private fun rememberTabEntries(
    tab: String,
    backStack: NavBackStack<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    val savedState = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val viewModels = rememberViewModelStoreNavEntryDecorator<NavKey>(
        rememberViewModelStoreProvider(key = "tab:$tab")
    )
    return rememberDecoratedNavEntries(backStack, listOf(savedState, viewModels), entryProvider)
}
