package my.naqibrafiqi.pustaka.ui.feature.library.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.BookCard
import my.naqibrafiqi.pustaka.ui.components.BookCover
import my.naqibrafiqi.pustaka.ui.components.ErrorState
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.components.SelectChip
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.components.TopBarActionButton
import my.naqibrafiqi.pustaka.ui.model.BookItem
import my.naqibrafiqi.pustaka.ui.model.BookState
import my.naqibrafiqi.pustaka.ui.model.booksLabel
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSelectFilter: (LibraryFilter) -> Unit,
    onToggleLayout: () -> Unit,
    onRetry: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onResumeBook: (Long) -> Unit,
    onSearch: () -> Unit,
) {
    LibraryScreenContent(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onSelectFilter = onSelectFilter,
        onToggleLayout = onToggleLayout,
        onRetry = onRetry,
        onOpenBook = onOpenBook,
        onResumeBook = onResumeBook,
        onSearch = onSearch,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreenContent(
    state: LibraryUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSelectFilter: (LibraryFilter) -> Unit,
    onToggleLayout: () -> Unit,
    onRetry: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onResumeBook: (Long) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isGrid = (state as? LibraryUiState.Ready)?.isGrid ?: true

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = "Library",
                actionButtons = {
                    TopBarActionButton(
                        onClick = onSearch,
                        iconResId = R.drawable.search,
                        contentDescription = "Search",
                    )

                    TopBarActionButton(
                        onClick = onToggleLayout,
                        iconResId = if (isGrid) R.drawable.list else R.drawable.grid,
                        contentDescription = if (isGrid) "Show as list" else "Show as grid",
                    )
                }
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            if (state is LibraryUiState.Loading) {
                LoadingState()
                return@PullToRefreshBox
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp)
            ) {
                when (state) {
                    is LibraryUiState.Error -> item { ErrorState(message = state.message, onRetry = onRetry) }
                    is LibraryUiState.Ready -> readyContent(state, onSelectFilter, onOpenBook, onResumeBook)
                    LibraryUiState.Loading -> Unit
                }
            }
        }
    }
}

private fun LazyListScope.readyContent(
    state: LibraryUiState.Ready,
    onSelectFilter: (LibraryFilter) -> Unit,
    onOpenBook: (Long) -> Unit,
    onResumeBook: (Long) -> Unit,
) {
    stickyHeader {
        FilterChipsScrollableRow(
            selected = state.filter,
            counts = state.counts,
            onSelect = onSelectFilter,
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .padding(
                    top = 8.dp,
                    bottom = 16.dp
                )
        )
    }

    if (state.isLibraryEmpty) {
        item {
            MessageState(
                title = "Your library is empty",
                message = "Copy EPUB files into ${state.libraryFolderPath}, then pull down to refresh. " +
                    "Each folder in it becomes a series.",
                iconRes = R.drawable.local_library,
            )
        }
        return
    }

    item {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Currently Reading",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )

                state.currentlyReading?.readAgo?.let { readAgo ->
                    Text(
                        text = readAgo,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = .8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            val current = state.currentlyReading
            if (current != null) {
                CurrentlyReadingCard(
                    current = current,
                    onOpen = { onOpenBook(current.book.id) },
                    onResume = { onResumeBook(current.book.id) },
                )
            } else {
                NothingInProgressCard()
            }
        }
    }

    stickyHeader {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (state.filter == LibraryFilter.ALL) "Recently Added" else state.filter.label,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "(${booksLabel(state.books.size)})",
                color = MaterialTheme.colorScheme.secondary.copy(alpha = .8f),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    if (state.books.isEmpty()) {
        item {
            MessageState(
                title = if (state.filter == LibraryFilter.READING) "Nothing in progress" else "No unread books",
                message = if (state.filter == LibraryFilter.READING) {
                    "Books you've started reading show up here."
                } else {
                    "You've started every book in your library."
                },
                iconRes = R.drawable.book_5,
            )
        }
    } else if (state.isGrid) {
        items(items = state.books.chunked(2), key = { row -> row.first().id }) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { book ->
                    BookCard(
                        book = book,
                        onClick = { onOpenBook(book.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    } else {
        items(items = state.books, key = { it.id }) { book ->
            BookCard(
                book = book,
                onClick = { onOpenBook(book.id) },
                isGrid = false,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun FilterChipsScrollableRow(
    selected: LibraryFilter,
    counts: Map<LibraryFilter, Int>,
    onSelect: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier)

        LibraryFilter.entries.forEach { filter ->
            SelectChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = "${filter.label} (${counts[filter] ?: 0})"
            )
        }

        Spacer(modifier = Modifier)
    }
}

@Composable
private fun CurrentlyReadingCard(
    current: CurrentlyReading,
    onOpen: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val book = current.book

    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.width(100.dp)) {
                BookCover(imageUrl = book.coverPath)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.titleLarge
                )

                book.credits?.let { credits ->
                    Text(
                        text = credits,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // The chapter is known once the reader saves positions (Phase 4). Until
                        // then, a volume shows its number and a standalone book shows nothing here.
                        book.volumeLabel?.let { volume ->
                            Icon(
                                painter = painterResource(id = R.drawable.book_5),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Text(
                                text = volume,
                                fontWeight = FontWeight.Normal,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Text(
                        text = "${book.percent}% complete",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimary,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    gapSize = 0.dp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = current.pageLabel.orEmpty(),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )

                Button(onClick = onResume) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.play_arrow),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )

                        Text(
                            text = "Resume",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NothingInProgressCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.menu_book),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Nothing in progress. The book you're reading will show up here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private val previewBooks = List(5) { index ->
    BookItem(
        id = index.toLong(),
        title = "The Left Hand of Darkness",
        credits = "Ursula K. Le Guin",
        coverPath = null,
        state = if (index == 0) BookState.READING else BookState.UNREAD,
        percent = if (index == 0) 42 else 0,
        volumeLabel = null,
    )
}

@Preview(showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    PustakaTheme {
        LibraryScreenContent(
            state = LibraryUiState.Ready(
                currentlyReading = CurrentlyReading(previewBooks[0], "Read 32m ago", "Page 131 of 312"),
                filter = LibraryFilter.ALL,
                counts = mapOf(LibraryFilter.ALL to 5, LibraryFilter.READING to 1, LibraryFilter.UNREAD to 4),
                books = previewBooks,
                isGrid = true,
                libraryFolderPath = "/storage/emulated/0/Pustaka",
            ),
            isRefreshing = false,
            onRefresh = {},
            onSelectFilter = {},
            onToggleLayout = {},
            onRetry = {},
            onOpenBook = {},
            onResumeBook = {},
            onSearch = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryScreenEmptyPreview() {
    PustakaTheme {
        LibraryScreenContent(
            state = LibraryUiState.Ready(
                currentlyReading = null,
                filter = LibraryFilter.ALL,
                counts = LibraryFilter.entries.associateWith { 0 },
                books = emptyList(),
                isGrid = true,
                libraryFolderPath = "/storage/emulated/0/Pustaka",
            ),
            isRefreshing = false,
            onRefresh = {},
            onSelectFilter = {},
            onToggleLayout = {},
            onRetry = {},
            onOpenBook = {},
            onResumeBook = {},
            onSearch = {},
        )
    }
}
