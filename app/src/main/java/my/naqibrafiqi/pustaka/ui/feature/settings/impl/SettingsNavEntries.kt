package my.naqibrafiqi.pustaka.ui.feature.settings.impl

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.ui.feature.settings.api.SettingsRoute
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.SettingsScreen
import my.naqibrafiqi.pustaka.ui.scan.LibraryScanViewModel

fun EntryProviderScope<NavKey>.settingsEntryBuilder(scanViewModel: LibraryScanViewModel) {
    entry<SettingsRoute> {
        val folderPath by scanViewModel.libraryFolderPath.collectAsStateWithLifecycle()
        val isScanning by scanViewModel.isScanning.collectAsStateWithLifecycle()

        SettingsScreen(
            libraryFolderPath = folderPath,
            isScanning = isScanning,
            onRescan = scanViewModel::refresh,
        )
    }
}
