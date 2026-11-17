package my.naqibrafiqi.pustaka.ui.feature.search.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.centredContentWidth
import my.naqibrafiqi.pustaka.ui.components.BookCover
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asString
import my.naqibrafiqi.pustaka.util.text.asUiText

@Composable
fun SearchScreen(
    query: String,
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onOpenSeries: (Long) -> Unit,
    onOpenBook: (Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = { SearchBar(query = query, onQueryChange = onQueryChange, onNavigateBack = onNavigateBack) },
    ) { innerPadding ->
        val modifier = Modifier
            .padding(innerPadding)
            .imePadding()
        when (state) {
            SearchUiState.Idle -> MessageState(
                title = stringResource(R.string.search_idle_title),
                message = stringResource(R.string.search_idle_message),
                iconRes = R.drawable.search,
                modifier = modifier,
            )
            is SearchUiState.Error -> MessageState(
                title = stringResource(R.string.common_error_title),
                message = stringResource(state.message),
                iconRes = R.drawable.broken_image,
                modifier = modifier,
            )
            is SearchUiState.Results -> if (state.results.isEmpty) {
                MessageState(
                    title = stringResource(R.string.search_no_matches_title),
                    message = stringResource(R.string.search_no_matches_message, state.query),
                    iconRes = R.drawable.search,
                    modifier = modifier,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .centredContentWidth()
                        .imePadding(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    ),
                ) {
                    section(R.string.series_title, "series", state.results.series, onOpenSeries)
                    section(R.string.search_books, "book", state.results.books, onOpenBook)
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onNavigateBack: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.common_navigate_back))
            }
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(painterResource(R.drawable.close), contentDescription = stringResource(R.string.search_clear))
                        }
                    }
                } else {
                    null
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun LazyListScope.section(
    @StringRes title: Int,
    keyPrefix: String,
    results: List<SearchResult>,
    onOpen: (Long) -> Unit,
) {
    if (results.isEmpty()) return
    item(key = "header-$keyPrefix") {
        Text(
            text = stringResource(R.string.library_filter_count, stringResource(title), results.size),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                .semantics { heading() },
        )
    }
    items(results, key = { "$keyPrefix-${it.id}" }) { result ->
        ListItem(
            headlineContent = { Text(result.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                val lines = listOfNotNull(result.subtitle?.asString(), result.matchedOn?.asString())
                if (lines.isNotEmpty()) Text(lines.joinToString("\n"), maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            leadingContent = { BookCover(imageUrl = result.coverPath, modifier = Modifier.width(40.dp)) },
            modifier = Modifier.clickable(onClickLabel = stringResource(R.string.book_open_details)) { onOpen(result.id) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    PustakaTheme {
        SearchScreen(
            query = "fantasy",
            state = SearchUiState.Results(
                query = "fantasy",
                results = SearchResults(
                    series = listOf(SearchResult(1, "Brunhild the Dragonslayer", "Yuiko Agarizaki".asUiText(), "Genre: Fantasy".asUiText(), null)),
                    books = listOf(SearchResult(2, "Brunhild, Vol. 01", "In Brunhild the Dragonslayer".asUiText(), "Genre: Fantasy".asUiText(), null)),
                ),
            ),
            onQueryChange = {},
            onOpenSeries = {},
            onOpenBook = {},
            onNavigateBack = {},
        )
    }
}
