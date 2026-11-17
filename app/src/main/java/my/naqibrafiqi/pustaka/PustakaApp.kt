package my.naqibrafiqi.pustaka

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
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
import my.naqibrafiqi.pustaka.ui.components.BottomBarRoute
import my.naqibrafiqi.pustaka.ui.components.SideRail
import my.naqibrafiqi.pustaka.ui.components.SideRailInsets
import my.naqibrafiqi.pustaka.ui.components.WideWindowWidth
import my.naqibrafiqi.pustaka.ui.feature.highlights.api.HighlightsRoute
import my.naqibrafiqi.pustaka.ui.feature.highlights.impl.highlightsEntryBuilder
import my.naqibrafiqi.pustaka.ui.feature.library.api.LibraryRoute
import my.naqibrafiqi.pustaka.ui.feature.library.impl.libraryEntryBuilder
import my.naqibrafiqi.pustaka.ui.feature.search.impl.searchEntryBuilder
import my.naqibrafiqi.pustaka.ui.feature.series.api.SeriesRoute
import my.naqibrafiqi.pustaka.ui.feature.series.impl.seriesEntryBuilder
import my.naqibrafiqi.pustaka.ui.feature.settings.api.SettingsRoute
import my.naqibrafiqi.pustaka.ui.feature.settings.impl.settingsEntryBuilder
import my.naqibrafiqi.pustaka.ui.scan.LibraryScanViewModel
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator
import my.naqibrafiqi.pustaka.util.text.resolve

@Composable
fun PustakaApp() {
    // Shared by the whole app, so the screens' pull-to-refresh and this message use the same one.
    val scanViewModel: LibraryScanViewModel = hiltViewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(scanViewModel) {
        scanViewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(resources)) }
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
        searchEntryBuilder()
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

    val currentItem = tabs.find { it.route == selectedTabRoute }
    val onSelectTab: (Int, BottomBarRoute) -> Unit = { index, _ -> selectedTabIndex = index }

    CompositionLocalProvider(LocalAppNavigator provides currentBackStack) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // A wide window gets the tabs in a rail down the side, and keeps it on every screen (D29).
            // A phone keeps the bottom bar, which hides on the screens opened from a tab.
            val useRail = maxWidth >= WideWindowWidth
            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    SideRail(currentItem = currentItem, items = tabs, onClick = onSelectTab)
                }

                Scaffold(
                    modifier = Modifier
                        .weight(1f)
                        // The rail keeps clear of the start side, so the screens don't need to.
                        .then(if (useRail) Modifier.consumeWindowInsets(SideRailInsets.only(WindowInsetsSides.Start)) else Modifier),
                    contentWindowInsets = if (useRail) {
                        ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    } else {
                        ScaffoldDefaults.contentWindowInsets
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!useRail) {
                            AnimatedVisibility(
                                visible = isMainScreen,
                                enter = slideInVertically { it },
                                exit = slideOutVertically { it }
                            ) {
                                BottomBar(currentItem = currentItem, items = tabs, onClick = onSelectTab)
                            }
                        }
                    }
                ) { innerPadding ->
                    NavDisplay(
                        entries = currentEntries,
                        // Consumed here, so the screens' own scaffolds don't leave the same space again.
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding),
                        onBack = {
                            currentBackStack.removeLastOrNull()
                        },
                    )
                }
            }
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
