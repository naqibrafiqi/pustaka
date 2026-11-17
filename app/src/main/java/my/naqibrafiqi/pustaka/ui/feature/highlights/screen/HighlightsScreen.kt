package my.naqibrafiqi.pustaka.ui.feature.highlights.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.model.Highlight
import my.naqibrafiqi.pustaka.data.model.HighlightColor
import my.naqibrafiqi.pustaka.ui.components.centredContentWidth
import my.naqibrafiqi.pustaka.ui.components.ErrorState
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.components.TopBarActionButton
import my.naqibrafiqi.pustaka.ui.components.menu.DropdownMenuButton
import my.naqibrafiqi.pustaka.ui.components.menu.MenuItem
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Highlights tab: every highlight in the library, grouped by series and book. Tapping one opens
 * the book at that spot.
 */
@Composable
fun HighlightsScreen(
    state: HighlightsUiState,
    snackbarHostState: SnackbarHostState,
    onSearch: (String) -> Unit,
    onRetry: () -> Unit,
    onOpen: (Highlight) -> Unit,
    onEdit: (Long) -> Unit,
    onExportBook: (Long) -> Unit,
    onExportAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val ready = state as? HighlightsUiState.Ready

    fun closeSearch() {
        searchOpen = false
        onSearch("")
    }
    BackHandler(enabled = searchOpen) { closeSearch() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (searchOpen) {
                SearchBar(query = ready?.query.orEmpty(), onQueryChange = onSearch, onClose = ::closeSearch)
            } else {
                HighlightsTopBar(
                    count = ready?.totalCount ?: 0,
                    onOpenSearch = { searchOpen = true },
                    onExportAll = onExportAll,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            when (state) {
                HighlightsUiState.Loading -> LoadingState()
                is HighlightsUiState.Error -> ErrorState(message = stringResource(state.message), onRetry = onRetry)
                is HighlightsUiState.Ready -> when {
                    state.totalCount == 0 -> MessageState(
                        title = stringResource(R.string.highlights_empty_title),
                        message = stringResource(R.string.highlights_empty_message),
                        iconRes = R.drawable.bookmark_stacks,
                    )
                    state.groups.isEmpty() -> MessageState(
                        title = stringResource(R.string.highlights_no_matches_title),
                        message = stringResource(R.string.highlights_no_matches_message, state.query.trim()),
                        iconRes = R.drawable.search,
                    )
                    else -> HighlightsList(
                        groups = state.groups,
                        onOpen = onOpen,
                        onEdit = onEdit,
                        onExportBook = onExportBook,
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightsTopBar(count: Int, onOpenSearch: () -> Unit, onExportAll: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    TopBar(
        title = stringResource(R.string.highlights_title),
        subtitle = if (count == 0) null else pluralStringResource(R.plurals.highlights_count, count, count),
        actionButtons = if (count == 0) {
            null
        } else {
            {
                TopBarActionButton(
                    onClick = onOpenSearch,
                    iconResId = R.drawable.search,
                    contentDescription = stringResource(R.string.highlights_search),
                )
                DropdownMenuButton(
                    expanded = menuOpen,
                    openMenu = { menuOpen = true },
                    closeMenu = { menuOpen = false },
                ) {
                    MenuItem(
                        onClick = {
                            menuOpen = false
                            onExportAll()
                        },
                        text = stringResource(R.string.highlights_export_all),
                        leadingIconResId = R.drawable.share,
                    )
                }
            }
        },
    )
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.search_close))
            }
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.highlights_search_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(painter = painterResource(R.drawable.close), contentDescription = stringResource(R.string.search_clear))
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
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
        }
    }
}

@Composable
private fun HighlightsList(
    groups: List<SeriesHighlights>,
    onOpen: (Highlight) -> Unit,
    onEdit: (Long) -> Unit,
    onExportBook: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .centredContentWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { group ->
            item(key = "series:${group.seriesId}") {
                Text(
                    text = group.name ?: stringResource(R.string.highlights_standalone),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .semantics { heading() },
                )
            }
            group.books.forEach { book ->
                item(key = "book:${book.bookId}") {
                    BookHeader(book = book, onExport = { onExportBook(book.bookId) })
                }
                items(book.highlights, key = { "highlight:${it.id}" }) { item ->
                    HighlightCard(item = item, onOpen = { onOpen(item.highlight) }, onEdit = { onEdit(item.id) })
                }
            }
        }
    }
}

@Composable
private fun BookHeader(book: BookHighlights, onExport: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = pluralStringResource(R.plurals.highlights_count, book.highlights.size, book.highlights.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onExport) {
            Icon(
                painter = painterResource(R.drawable.share),
                contentDescription = stringResource(R.string.highlights_export_book, book.title),
            )
        }
    }
}

@Composable
private fun HighlightCard(item: HighlightItem, onOpen: () -> Unit, onEdit: () -> Unit) {
    val highlight = item.highlight
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.highlights_open_in_book), onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(start = 16.dp, top = 16.dp, bottom = 8.dp, end = 4.dp),
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.small)
                    .background(Color(highlight.color.argb))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = highlight.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                )
                highlight.note?.let { note ->
                    Text(text = note, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = listOfNotNull(
                            item.page?.let { stringResource(R.string.page_number, it) },
                            DATE.format(highlight.createdAt.atZone(ZoneId.systemDefault())),
                        ).joinToString(stringResource(R.string.common_dot_join)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onEdit) {
                        Icon(painter = painterResource(R.drawable.edit), contentDescription = stringResource(R.string.highlights_edit))
                    }
                }
            }
        }
    }
}

private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

@Preview(showBackground = true)
@Composable
private fun HighlightsScreenPreview() {
    fun item(id: Long, text: String, color: HighlightColor, note: String? = null, page: Int) = HighlightItem(
        highlight = Highlight(
            id = id,
            bookId = 1,
            locator = "{}",
            text = text,
            note = note,
            color = color,
            createdAt = Instant.parse("2026-11-14T10:00:00Z"),
            updatedAt = Instant.parse("2026-11-14T10:00:00Z"),
        ),
        page = page,
        progression = 0.0,
    )
    val groups = listOf(
        SeriesHighlights(
            seriesId = 1,
            name = "Brunhild",
            books = listOf(
                BookHighlights(
                    bookId = 1,
                    title = "Brunhild, Vol. 01",
                    highlights = listOf(
                        item(1, "The dragon's maiden looked up at the sky and smiled.", HighlightColor.YELLOW, page = 12),
                        item(2, "Not every story ends with a battle.", HighlightColor.GREEN, note = "Foreshadowing?", page = 48),
                    ),
                ),
            ),
        ),
    )
    PustakaTheme {
        HighlightsScreen(
            state = HighlightsUiState.Ready(allGroups = groups, groups = groups, query = ""),
            snackbarHostState = remember { SnackbarHostState() },
            onSearch = {},
            onRetry = {},
            onOpen = {},
            onEdit = {},
            onExportBook = {},
            onExportAll = {},
        )
    }
}
