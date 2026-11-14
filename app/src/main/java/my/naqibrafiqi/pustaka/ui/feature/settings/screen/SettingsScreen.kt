package my.naqibrafiqi.pustaka.ui.feature.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

/**
 * Settings. For now this only has the library section, so the library can be rescanned. The rest
 * of Settings comes in Phase 6.
 */
@Composable
fun SettingsScreen(
    libraryFolderPath: String?,
    isScanning: Boolean,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopBar(title = "Settings") },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            )
            ListItem(
                headlineContent = { Text("Library folder") },
                supportingContent = { Text(libraryFolderPath.orEmpty()) },
                leadingContent = { Icon(painterResource(R.drawable.folder_managed), contentDescription = null) },
            )
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
                modifier = Modifier.clickable(enabled = !isScanning, onClick = onRescan),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PustakaTheme {
        SettingsScreen(libraryFolderPath = "/storage/emulated/0/Pustaka", isScanning = false, onRescan = {})
    }
}
