package my.naqibrafiqi.pustaka.ui.feature.settings.impl

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.data.settings.AppTheme
import my.naqibrafiqi.pustaka.ui.feature.settings.api.AboutRoute
import my.naqibrafiqi.pustaka.ui.feature.settings.api.HiddenItemsRoute
import my.naqibrafiqi.pustaka.ui.feature.settings.api.LicenceRoute
import my.naqibrafiqi.pustaka.ui.feature.settings.api.LicencesRoute
import my.naqibrafiqi.pustaka.ui.feature.settings.api.ReaderDefaultsRoute
import my.naqibrafiqi.pustaka.ui.feature.settings.api.SettingsRoute
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.SettingsActions
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.SettingsEvent
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.SettingsScreen
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.SettingsViewModel
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.about.AboutScreen
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.about.LicenceScreen
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.about.LicencesScreen
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.about.OpenSourceLibraries
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.hidden.HiddenItemsScreen
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.hidden.HiddenItemsViewModel
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.reader.ReaderDefaultsScreen
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.reader.ReaderDefaultsViewModel
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.rememberFolderPicker
import my.naqibrafiqi.pustaka.ui.scan.LibraryScanViewModel
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator

fun EntryProviderScope<NavKey>.settingsEntryBuilder(scanViewModel: LibraryScanViewModel) {
    entry<SettingsRoute> {
        val navigator = LocalAppNavigator.current
        val viewModel: SettingsViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val storage by viewModel.storage.collectAsStateWithLifecycle()
        val folderChange by viewModel.folderChange.collectAsStateWithLifecycle()
        val isScanning by scanViewModel.isScanning.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val pickFolder = rememberFolderPicker(
            onPicked = viewModel::onFolderPicked,
            onUnavailable = viewModel::onFolderPickerUnavailable,
        )

        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is SettingsEvent.Message -> snackbarHostState.showSnackbar(event.text)
                    SettingsEvent.Rescan -> scanViewModel.refresh()
                }
            }
        }

        val actions = remember(viewModel, navigator) {
            object : SettingsActions {
                override fun chooseFolder() = pickFolder()
                override fun useDefaultFolder() = viewModel.useDefaultFolder()
                override fun rescan() = scanViewModel.refresh()
                override fun openHiddenItems() { navigator.add(HiddenItemsRoute) }
                override fun setAppTheme(theme: AppTheme) = viewModel.setAppTheme(theme)
                override fun setDynamicColor(enabled: Boolean) = viewModel.setDynamicColor(enabled)
                override fun openReaderSettings() { navigator.add(ReaderDefaultsRoute) }
                override fun clearCovers() = viewModel.clearCovers()
                override fun openAbout() { navigator.add(AboutRoute) }
                override fun openLicences() { navigator.add(LicencesRoute) }
            }
        }

        SettingsScreen(
            state = state,
            storage = storage,
            isScanning = isScanning,
            folderChange = folderChange,
            snackbarHostState = snackbarHostState,
            actions = actions,
            onConfirmFolderChange = viewModel::confirmFolderChange,
            onDismissFolderChange = viewModel::dismissFolderChange,
        )
    }

    entry<HiddenItemsRoute> {
        val navigator = LocalAppNavigator.current
        val viewModel: HiddenItemsViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(viewModel) {
            viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
        }

        HiddenItemsScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onRestore = viewModel::restore,
            onRetry = viewModel::retry,
            onNavigateBack = { navigator.removeLastOrNull() },
        )
    }

    entry<ReaderDefaultsRoute> {
        val navigator = LocalAppNavigator.current
        val viewModel: ReaderDefaultsViewModel = hiltViewModel()
        val settings by viewModel.settings.collectAsStateWithLifecycle()

        ReaderDefaultsScreen(
            settings = settings,
            onChange = viewModel::update,
            onReset = viewModel::reset,
            onNavigateBack = { navigator.removeLastOrNull() },
        )
    }

    entry<AboutRoute> {
        val navigator = LocalAppNavigator.current
        AboutScreen(
            onOpenLicences = { navigator.add(LicencesRoute) },
            onNavigateBack = { navigator.removeLastOrNull() },
        )
    }

    entry<LicencesRoute> {
        val navigator = LocalAppNavigator.current
        LicencesScreen(
            onOpenLibrary = { index -> navigator.add(LicenceRoute(index)) },
            onNavigateBack = { navigator.removeLastOrNull() },
        )
    }

    entry<LicenceRoute> { route ->
        val navigator = LocalAppNavigator.current
        val library = OpenSourceLibraries.getOrNull(route.libraryIndex)
        if (library == null) {
            LaunchedEffect(Unit) { navigator.removeLastOrNull() }
        } else {
            LicenceScreen(library = library, onNavigateBack = { navigator.removeLastOrNull() })
        }
    }
}
