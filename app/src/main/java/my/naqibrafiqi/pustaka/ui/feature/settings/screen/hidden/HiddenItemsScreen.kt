package my.naqibrafiqi.pustaka.ui.feature.settings.screen.hidden

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.centredContentWidth
import my.naqibrafiqi.pustaka.ui.components.BookCover
import my.naqibrafiqi.pustaka.ui.components.ErrorState
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asString
import my.naqibrafiqi.pustaka.util.text.asUiText

@Composable
fun HiddenItemsScreen(
    state: HiddenItemsUiState,
    snackbarHostState: SnackbarHostState,
    onRestore: (HiddenItem) -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = { TopBar(title = stringResource(R.string.settings_hidden_items), onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (state) {
            HiddenItemsUiState.Loading -> LoadingState(Modifier.padding(innerPadding))
            is HiddenItemsUiState.Error -> ErrorState(stringResource(state.message), onRetry, Modifier.padding(innerPadding))
            is HiddenItemsUiState.Ready -> if (state.isEmpty) {
                MessageState(
                    title = stringResource(R.string.hidden_empty_title),
                    message = stringResource(R.string.hidden_empty_message),
                    iconRes = R.drawable.local_library,
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .centredContentWidth(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    ),
                ) {
                    section(R.string.series_title, state.series, onRestore)
                    section(R.string.search_books, state.books, onRestore)
                }
            }
        }
    }
}

private fun LazyListScope.section(@StringRes title: Int, items: List<HiddenItem>, onRestore: (HiddenItem) -> Unit) {
    if (items.isEmpty()) return
    item(key = "header-$title") {
        Text(
            text = stringResource(R.string.library_filter_count, stringResource(title), items.size),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                .semantics { heading() },
        )
    }
    items(items, key = { (if (it.isSeries) "series-" else "book-") + it.id }) { item ->
        ListItem(
            headlineContent = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(item.description.asString(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingContent = { BookCover(imageUrl = item.coverPath, modifier = Modifier.width(40.dp)) },
            trailingContent = {
                val restoreDescription = stringResource(R.string.hidden_restore_named, item.name)
                OutlinedButton(
                    onClick = { onRestore(item) },
                    modifier = Modifier.semantics { contentDescription = restoreDescription },
                ) { Text(stringResource(R.string.hidden_restore)) }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HiddenItemsScreenPreview() {
    PustakaTheme {
        HiddenItemsScreen(
            state = HiddenItemsUiState.Ready(
                series = listOf(HiddenItem(1, true, "Brunhild the Dragonslayer", "Folder: Brunhild".asUiText(), null)),
                books = listOf(
                    HiddenItem(2, false, "Battle Divas", "Standalone book".asUiText(), null),
                    HiddenItem(3, false, "Disowned but Not Disheartened! Vol. 2", "In Disowned but Not Disheartened!".asUiText(), null),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onRestore = {},
            onRetry = {},
            onNavigateBack = {},
        )
    }
}
