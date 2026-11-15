package my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.settings.SeriesLayout
import my.naqibrafiqi.pustaka.data.settings.SeriesSort
import my.naqibrafiqi.pustaka.ui.components.BookCover
import my.naqibrafiqi.pustaka.ui.components.BookSeriesCard
import my.naqibrafiqi.pustaka.ui.components.ErrorState
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.components.TopBarActionButton
import my.naqibrafiqi.pustaka.ui.components.menu.MenuItem
import my.naqibrafiqi.pustaka.ui.model.SeriesItem
import my.naqibrafiqi.pustaka.ui.model.volumesLabel
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asString
import my.naqibrafiqi.pustaka.util.text.asUiText

@Composable
fun SeriesScreen(
    state: SeriesUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenSeries: (Long) -> Unit,
    onOpenBook: (Long) -> Unit,
    onReadBook: (Long) -> Unit,
    createDialog: CreateSeriesDialog?,
    onNewSeries: () -> Unit,
    onNewNameChange: (String) -> Unit,
    onCreateSeries: () -> Unit,
    onDismissCreate: () -> Unit,
    onSearch: () -> Unit,
    onSelectSort: (SeriesSort) -> Unit,
    onToggleLayout: () -> Unit,
) {
    SeriesScreenContent(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onRetry = onRetry,
        onOpenSeries = onOpenSeries,
        onOpenBook = onOpenBook,
        onReadBook = onReadBook,
        onNewSeries = onNewSeries,
        onSearch = onSearch,
        onSelectSort = onSelectSort,
        onToggleLayout = onToggleLayout,
    )

    if (createDialog != null) {
        CreateSeriesDialogContent(
            dialog = createDialog,
            libraryFolderPath = (state as? SeriesUiState.Ready)?.libraryFolderPath,
            onNameChange = onNewNameChange,
            onCreate = onCreateSeries,
            onDismiss = onDismissCreate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeriesScreenContent(
    state: SeriesUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenSeries: (Long) -> Unit,
    onOpenBook: (Long) -> Unit,
    onReadBook: (Long) -> Unit,
    onNewSeries: () -> Unit,
    onSearch: () -> Unit,
    onSelectSort: (SeriesSort) -> Unit,
    onToggleLayout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val ready = state as? SeriesUiState.Ready
    val layout = ready?.layout ?: SeriesLayout.CARDS

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (state is SeriesUiState.Ready && state.series.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onNewSeries,
                    icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
                    text = { Text(stringResource(R.string.series_new)) },
                )
            }
        },
        topBar = {
            TopBar(
                title = stringResource(R.string.series_title),
                actionButtons = {
                    TopBarActionButton(
                        onClick = onSearch,
                        iconResId = R.drawable.search,
                        contentDescription = stringResource(R.string.common_search),
                    )

                    TopBarActionButton(
                        onClick = onToggleLayout,
                        iconResId = if (layout == SeriesLayout.CARDS) R.drawable.grid else R.drawable.list,
                        contentDescription = stringResource(
                            if (layout == SeriesLayout.CARDS) R.string.layout_show_compact_grid else R.string.layout_show_cards
                        ),
                    )

                    SortMenu(selected = ready?.sort ?: SeriesSort.NAME, onSelect = onSelectSort)
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            if (state is SeriesUiState.Loading) {
                LoadingState()
                return@PullToRefreshBox
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                // Leaves room under the last card for the "New series" button.
                contentPadding = PaddingValues(top = 8.dp, bottom = innerPadding.calculateBottomPadding() + 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state) {
                    is SeriesUiState.Error -> item { ErrorState(message = stringResource(state.message), onRetry = onRetry) }
                    is SeriesUiState.Ready -> if (state.series.isEmpty()) {
                        item {
                            MessageState(
                                title = stringResource(R.string.series_empty_title),
                                message = stringResource(R.string.series_empty_message, state.libraryFolderPath),
                                iconRes = R.drawable.newsstand,
                                actionLabel = stringResource(R.string.series_new),
                                onAction = onNewSeries,
                            )
                        }
                    } else if (state.layout == SeriesLayout.GRID) {
                        items(items = state.series.chunked(GRID_COLUMNS), key = { row -> row.first().id }) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                row.forEach { series ->
                                    SeriesGridItem(
                                        series = series,
                                        onClick = { onOpenSeries(series.id) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(GRID_COLUMNS - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    } else {
                        items(items = state.series, key = { it.id }) { series ->
                            BookSeriesCard(
                                series = series,
                                onContinueReading = { series.continueBookId?.let(onReadBook) },
                                onViewSeries = { onOpenSeries(series.id) },
                                onOpenVolume = onOpenBook,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                    SeriesUiState.Loading -> Unit
                }
            }
        }
    }
}

private const val GRID_COLUMNS = 3

/** The sort button and its menu, with a tick next to the current order. */
@Composable
private fun SortMenu(selected: SeriesSort, onSelect: (SeriesSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TopBarActionButton(
            onClick = { expanded = true },
            iconResId = R.drawable.sort,
            contentDescription = stringResource(R.string.series_sort),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = stringResource(R.string.series_sort_by),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SeriesSort.entries.forEach { sort ->
                MenuItem(
                    onClick = {
                        expanded = false
                        onSelect(sort)
                    },
                    text = stringResource(sort.label),
                    modifier = Modifier.semantics { this.selected = sort == selected },
                    trailingIconResId = if (sort == selected) R.drawable.check else null,
                )
            }
        }
    }
}

/** A series in the compact grid: its cover, name and how far through it you are. */
@Composable
private fun SeriesGridItem(series: SeriesItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClickLabel = stringResource(R.string.series_card_view_series), onClick = onClick)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BookCover(imageUrl = series.coverPath, modifier = Modifier.fillMaxWidth())
        Text(
            text = series.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(
                R.string.common_dot_separated,
                volumesLabel(series.volumeCount).asString(),
                stringResource(R.string.common_percent, series.percent),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SeriesScreenPreview() {
    PustakaTheme {
        SeriesScreenContent(
            state = SeriesUiState.Ready(
                series = List(3) { index ->
                    SeriesItem(
                        id = index.toLong(),
                        name = "Playing Death Games to Put Food on the Table",
                        author = "Yushi Ukai",
                        illustrator = "Nekometaru",
                        genres = listOf("Action", "Adventure"),
                        coverPath = null,
                        volumeCount = 6,
                        finishedCount = 1,
                        percent = 16,
                        currentLabel = "Reading: Volume 2".asUiText(),
                        continueBookId = 2,
                        preview = emptyList(),
                    )
                },
                libraryFolderPath = "/storage/emulated/0/Pustaka",
            ),
            isRefreshing = false,
            onRefresh = {},
            onRetry = {},
            onOpenSeries = {},
            onOpenBook = {},
            onReadBook = {},
            onNewSeries = {},
            onSearch = {},
            onSelectSort = {},
            onToggleLayout = {},
        )
    }
}

@Composable
private fun CreateSeriesDialogContent(
    dialog: CreateSeriesDialog,
    libraryFolderPath: String?,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.series_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(
                        R.string.series_new_message,
                        libraryFolderPath ?: stringResource(R.string.series_new_your_library_folder),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = dialog.name,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text(stringResource(R.string.series_new_folder_name)) },
                    singleLine = true,
                    isError = dialog.error != null,
                    supportingText = dialog.error?.let { error -> { Text(error.asString()) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onCreate() }),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCreate, enabled = !dialog.isCreating && dialog.name.isNotBlank()) {
                Text(stringResource(R.string.series_new_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
