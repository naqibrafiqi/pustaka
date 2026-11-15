package my.naqibrafiqi.pustaka.ui.feature.settings.screen.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

/** Where Pustaka's source code is. */
const val SOURCE_CODE_URL = "https://github.com/rafeeqy/pustaka"

@Composable
fun AboutScreen(onOpenLicences: () -> Unit, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    Scaffold(topBar = { TopBar(title = "About Pustaka", onNavigateBack = onNavigateBack) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Pustaka", style = MaterialTheme.typography.headlineMedium)
                version?.let {
                    Text(
                        text = "Version $it",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "An offline EPUB reader for books that come in series. It reads the EPUB files " +
                        "in a folder you manage yourself, and never renames, moves or uploads them.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            ListItem(
                headlineContent = { Text("Source code") },
                supportingContent = { Text(SOURCE_CODE_URL.removePrefix("https://")) },
                leadingContent = { Icon(painterResource(R.drawable.article), contentDescription = null) },
                modifier = Modifier.clickable { runCatching { uriHandler.openUri(SOURCE_CODE_URL) } },
            )
            ListItem(
                headlineContent = { Text("Open-source licences") },
                supportingContent = { Text("The libraries Pustaka is built with") },
                leadingContent = { Icon(painterResource(R.drawable.info), contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenLicences),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("How Pustaka is made", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Pustaka is vibe coded. Its code, tests and docs are written by Claude, Anthropic's " +
                        "AI model, working in Claude Code. The project owner decides what the app should do, " +
                        "and reviews and tries out each part before it's added.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun LicencesScreen(onOpenLibrary: (Int) -> Unit, onNavigateBack: () -> Unit) {
    Scaffold(topBar = { TopBar(title = "Open-source licences", onNavigateBack = onNavigateBack) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                Text(
                    text = "Pustaka is built with these open-source projects. Tap one to read its licence.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            itemsIndexed(OpenSourceLibraries, key = { _, library -> library.name }) { index, library ->
                ListItem(
                    headlineContent = { Text(library.name) },
                    overlineContent = { Text("${library.author} · ${library.licence.title}") },
                    supportingContent = { Text(library.description) },
                    modifier = Modifier.clickable { onOpenLibrary(index) },
                )
            }
        }
    }
}

/** One library's details and the full text of its licence. */
@Composable
fun LicenceScreen(library: OpenSourceLibrary, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val text = remember(library.licence) {
        reflowLicence(context.resources.openRawResource(library.licence.textRes).bufferedReader().use { it.readText() })
    }

    Scaffold(
        topBar = { TopBar(title = library.name, subtitle = library.licence.title, onNavigateBack = onNavigateBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(library.author) },
                supportingContent = { Text(library.url.removePrefix("https://")) },
                modifier = Modifier.clickable { runCatching { uriHandler.openUri(library.url) } },
            )
            SelectionContainer {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Licence files are wrapped at about 80 characters, which breaks lines in odd places on a phone.
 * This joins the lines of each paragraph, keeping paragraphs and list items ("* ", "1. ", "(a) ")
 * on lines of their own.
 */
internal fun reflowLicence(text: String): String =
    text.trim().lines().joinToString("\n").split(Regex("""\n\s*\n""")).joinToString("\n\n") { paragraph ->
        val lines = paragraph.lines().map { it.trim() }.filter { it.isNotEmpty() }
        buildString {
            lines.forEach { line ->
                if (isNotEmpty()) append(if (LIST_ITEM.containsMatchIn(line)) "\n" else " ")
                append(line)
            }
        }
    }

private val LIST_ITEM = Regex("""^(\*|-|\d+\.|\([a-z0-9]+\))\s""")

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    PustakaTheme { AboutScreen(onOpenLicences = {}, onNavigateBack = {}) }
}

@Preview(showBackground = true)
@Composable
private fun LicencesScreenPreview() {
    PustakaTheme { LicencesScreen(onOpenLibrary = {}, onNavigateBack = {}) }
}
