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
import androidx.compose.ui.semantics.Role
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
        topBar = { TopBar(title = "Settings") },
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
            SectionTitle("Library")
            SettingsItem(
                title = "Library folder",
                summary = state.libraryFolderPath,
                iconRes = R.drawable.folder_managed,
                onClick = actions::chooseFolder,
            )
            if (!state.isDefaultLibraryFolder) {
                SettingsItem(
                    title = "Use default folder",
                    summary = state.defaultLibraryFolderPath,
                    iconRes = R.drawable.undo,
                    onClick = actions::useDefaultFolder,
                )
            }
            ListItem(
                headlineContent = { Text("Rescan library") },
                supportingContent = {
                    Text(if (isScanning) "Scanning…" else "Look for new, changed and missing books")
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
                title = "Hidden items",
                summary = state.hiddenLabel,
                iconRes = R.drawable.bookmark_remove,
                onClick = actions::openHiddenItems,
            )

            SectionTitle("Appearance")
            SettingsItem(
                title = "Theme",
                summary = state.appTheme.label,
                iconRes = R.drawable.donut_large,
                onClick = { themeDialogOpen = true },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text("Dynamic colours") },
                    supportingContent = { Text("Use colours from your wallpaper") },
                    leadingContent = { Icon(painterResource(R.drawable.stylus_brush), contentDescription = null) },
                    trailingContent = { Switch(checked = state.dynamicColor, onCheckedChange = null) },
                    modifier = Modifier.toggleable(
                        value = state.dynamicColor,
                        role = Role.Switch,
                        onValueChange = actions::setDynamicColor,
                    ),
                )
            }

            SectionTitle("Reading")
            SettingsItem(
                title = "Reader settings",
                summary = "Theme, font, text size, spacing, margins and scrolling",
                iconRes = R.drawable.menu_book,
                onClick = actions::openReaderSettings,
            )

            SectionTitle("Storage")
            StorageItems(storage, isScanning, actions::clearCovers)

            SectionTitle("About")
            SettingsItem(
                title = "About Pustaka",
                summary = "Version, source code and how Pustaka is made",
                iconRes = R.drawable.info,
                onClick = actions::openAbout,
            )
            SettingsItem(
                title = "Open-source licences",
                summary = "The libraries Pustaka is built with",
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
            title = { Text(if (change.isDefault) "Use the default folder?" else "Use this folder?") },
            text = {
                Text(
                    "Pustaka will read your library from ${change.path}.\n\n" +
                        "Books that aren't in this folder stay in your library as missing, with their progress " +
                        "and highlights, and come back if their files turn up here. Nothing is moved or deleted."
                )
            },
            confirmButton = { TextButton(onClick = onConfirmFolderChange) { Text("Use folder") } },
            dismissButton = { TextButton(onClick = onDismissFolderChange) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StorageItems(storage: StorageReport?, isScanning: Boolean, onClearCovers: () -> Unit) {
    val measuring = "Measuring…"
    SettingsItem(
        title = "Books",
        summary = storage?.let { "${formatBytes(it.bookFiles)} in ${booksLabel(it.bookCount)}" } ?: measuring,
        iconRes = R.drawable.book_5,
    )
    ListItem(
        headlineContent = { Text("Cover cache") },
        supportingContent = {
            Text(
                when {
                    storage == null -> measuring
                    isScanning -> "${formatBytes(storage.covers)}. Covers are being read…"
                    else -> "${formatBytes(storage.covers)}. Cleared covers come back on the next scan."
                }
            )
        },
        leadingContent = { Icon(painterResource(R.drawable.broken_image), contentDescription = null) },
        trailingContent = {
            TextButton(
                onClick = onClearCovers,
                enabled = storage != null && storage.covers > 0 && !isScanning,
            ) { Text("Clear") }
        },
    )
    SettingsItem(
        title = "App data",
        summary = storage?.let { "${formatBytes(it.appData)}: progress, highlights, edits and settings" } ?: measuring,
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
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
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
        title = { Text("Theme") },
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
                        Text(theme.label, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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
