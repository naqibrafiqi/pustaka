package my.naqibrafiqi.pustaka.ui.feature.settings.screen

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.settings.AppTheme
import my.naqibrafiqi.pustaka.data.storage.StorageReport
import my.naqibrafiqi.pustaka.data.storage.formatBytes
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.model.booksLabel
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asString

/** What the Settings screen can do. */
interface SettingsActions {
    fun chooseFolder()
    fun useDefaultFolder()
    fun rescan()
    fun openHiddenItems()
    fun setAppTheme(theme: AppTheme)
    fun setDynamicColor(enabled: Boolean)
    fun openReaderSettings()
    fun clearCovers()
    fun openAbout()
    fun openLicences()
}

@Composable
fun SettingsScreen(
    state: SettingsUiState?,
    storage: StorageReport?,
    isScanning: Boolean,
    folderChange: FolderChange?,
    snackbarHostState: SnackbarHostState,
    actions: SettingsActions,
    onConfirmFolderChange: () -> Unit,
    onDismissFolderChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var themeDialogOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { TopBar(title = stringResource(R.string.navigation_label_settings)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state == null) {
            LoadingState(Modifier.padding(innerPadding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(stringResource(R.string.navigation_label_library))
            SettingsItem(
                title = stringResource(R.string.settings_library_folder),
                summary = state.libraryFolderPath,
                iconRes = R.drawable.folder_managed,
                onClick = actions::chooseFolder,
            )
            if (!state.isDefaultLibraryFolder) {
                SettingsItem(
                    title = stringResource(R.string.settings_use_default_folder),
                    summary = state.defaultLibraryFolderPath,
                    iconRes = R.drawable.undo,
                    onClick = actions::useDefaultFolder,
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rescan)) },
                supportingContent = {
                    Text(stringResource(if (isScanning) R.string.settings_scanning else R.string.settings_rescan_summary))
                },
                leadingContent = { Icon(painterResource(R.drawable.sync), contentDescription = null) },
                trailingContent = if (isScanning) {
                    { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp) }
                } else {
                    null
                },
                modifier = Modifier.clickable(enabled = !isScanning, onClick = actions::rescan),
            )
            SettingsItem(
                title = stringResource(R.string.settings_hidden_items),
                summary = state.hiddenLabel.asString(),
                iconRes = R.drawable.bookmark_remove,
                onClick = actions::openHiddenItems,
            )

            SectionTitle(stringResource(R.string.settings_appearance))
            SettingsItem(
                title = stringResource(R.string.reader_setting_theme),
                summary = stringResource(state.appTheme.label),
                iconRes = R.drawable.donut_large,
                onClick = { themeDialogOpen = true },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dynamic_colours)) },
                    supportingContent = { Text(stringResource(R.string.settings_dynamic_colours_summary)) },
                    leadingContent = { Icon(painterResource(R.drawable.stylus_brush), contentDescription = null) },
                    trailingContent = { Switch(checked = state.dynamicColor, onCheckedChange = null) },
                    modifier = Modifier.toggleable(
                        value = state.dynamicColor,
                        role = Role.Switch,
                        onValueChange = actions::setDynamicColor,
                    ),
                )
            }

            SectionTitle(stringResource(R.string.settings_reading))
            SettingsItem(
                title = stringResource(R.string.settings_reader_settings),
                summary = stringResource(R.string.settings_reader_settings_summary),
                iconRes = R.drawable.menu_book,
                onClick = actions::openReaderSettings,
            )

            SectionTitle(stringResource(R.string.settings_storage))
            StorageItems(storage, isScanning, actions::clearCovers)

            SectionTitle(stringResource(R.string.settings_about))
            SettingsItem(
                title = stringResource(R.string.about_title),
                summary = stringResource(R.string.settings_about_summary),
                iconRes = R.drawable.info,
                onClick = actions::openAbout,
            )
            SettingsItem(
                title = stringResource(R.string.licences_title),
                summary = stringResource(R.string.licences_summary),
                iconRes = R.drawable.article,
                onClick = actions::openLicences,
            )
        }
    }

    if (themeDialogOpen) {
        ThemeDialog(
            selected = state?.appTheme ?: AppTheme.SYSTEM,
            onSelect = {
                actions.setAppTheme(it)
                themeDialogOpen = false
            },
            onDismiss = { themeDialogOpen = false },
        )
    }

    folderChange?.let { change ->
        AlertDialog(
            onDismissRequest = onDismissFolderChange,
            title = {
                Text(stringResource(if (change.isDefault) R.string.settings_folder_change_default_title else R.string.settings_folder_change_title))
            },
            text = { Text(stringResource(R.string.settings_folder_change_message, change.path)) },
            confirmButton = { TextButton(onClick = onConfirmFolderChange) { Text(stringResource(R.string.settings_folder_change_confirm)) } },
            dismissButton = { TextButton(onClick = onDismissFolderChange) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun StorageItems(storage: StorageReport?, isScanning: Boolean, onClearCovers: () -> Unit) {
    val measuring = stringResource(R.string.settings_measuring)
    SettingsItem(
        title = stringResource(R.string.search_books),
        summary = storage?.let {
            stringResource(R.string.settings_storage_books, formatBytes(it.bookFiles), booksLabel(it.bookCount).asString())
        } ?: measuring,
        iconRes = R.drawable.book_5,
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_cover_cache)) },
        supportingContent = {
            Text(
                when {
                    storage == null -> measuring
                    isScanning -> stringResource(R.string.settings_cover_cache_reading, formatBytes(storage.covers))
                    else -> stringResource(R.string.settings_cover_cache_summary, formatBytes(storage.covers))
                }
            )
        },
        leadingContent = { Icon(painterResource(R.drawable.broken_image), contentDescription = null) },
        trailingContent = {
            TextButton(
                onClick = onClearCovers,
                enabled = storage != null && storage.covers > 0 && !isScanning,
            ) { Text(stringResource(R.string.settings_cover_cache_clear)) }
        },
    )
    SettingsItem(
        title = stringResource(R.string.settings_app_data),
        summary = storage?.let { stringResource(R.string.settings_app_data_summary, formatBytes(it.appData)) } ?: measuring,
        iconRes = R.drawable.storage,
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsItem(title: String, summary: String, iconRes: Int, onClick: (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(painterResource(iconRes), contentDescription = null) },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

@Composable
private fun ThemeDialog(selected: AppTheme, onSelect: (AppTheme) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_setting_theme)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AppTheme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = theme == selected, role = Role.RadioButton) { onSelect(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = theme == selected, onClick = null)
                        Text(stringResource(theme.label), modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun SettingsScreenPreview() {
    PustakaTheme {
        SettingsScreen(
            state = SettingsUiState(
                libraryFolderPath = "/storage/emulated/0/Download/Pustaka",
                defaultLibraryFolderPath = "/storage/emulated/0/Pustaka",
                isDefaultLibraryFolder = false,
                hiddenSeries = 1,
                hiddenBooks = 2,
                appTheme = AppTheme.SYSTEM,
                dynamicColor = false,
            ),
            storage = StorageReport(bookFiles = 182_400_000, bookCount = 8, covers = 3_400_000, appData = 512_000),
            isScanning = false,
            folderChange = null,
            snackbarHostState = remember { SnackbarHostState() },
            actions = object : SettingsActions {
                override fun chooseFolder() = Unit
                override fun useDefaultFolder() = Unit
                override fun rescan() = Unit
                override fun openHiddenItems() = Unit
                override fun setAppTheme(theme: AppTheme) = Unit
                override fun setDynamicColor(enabled: Boolean) = Unit
                override fun openReaderSettings() = Unit
                override fun clearCovers() = Unit
                override fun openAbout() = Unit
                override fun openLicences() = Unit
            },
            onConfirmFolderChange = {},
            onDismissFolderChange = {},
        )
    }
}
