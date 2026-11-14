package my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.BookSeriesCard
import my.naqibrafiqi.pustaka.ui.components.ErrorState
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.components.TopBarActionButton
import my.naqibrafiqi.pustaka.ui.model.SeriesItem
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun SeriesScreen(
    state: SeriesUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenSeries: (Long) -> Unit,
    onOpenBook: (Long) -> Unit,
) {
    SeriesScreenContent(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onRetry = onRetry,
        onOpenSeries = onOpenSeries,
        onOpenBook = onOpenBook,
    )
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
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = "Series",
                actionButtons = {
                    TopBarActionButton(
                        onClick = { /* TODO: ??? */ },
                        iconResId = R.drawable.search
                    )

                    TopBarActionButton(
                        onClick = { /* TODO: ??? */ },
                        iconResId = R.drawable.grid
                    )

                    TopBarActionButton(
                        onClick = { /* TODO: ??? */ },
                        iconResId = R.drawable.filter_list
                    )
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
                contentPadding = PaddingValues(top = 8.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state) {
                    is SeriesUiState.Error -> item { ErrorState(message = state.message, onRetry = onRetry) }
                    is SeriesUiState.Ready -> if (state.series.isEmpty()) {
                        item {
                            MessageState(
                                title = "No series yet",
                                message = "Each folder inside ${state.libraryFolderPath} becomes a series, " +
                                    "with its EPUB files as the volumes. Pull down to refresh.",
                                iconRes = R.drawable.newsstand,
                            )
                        }
                    } else {
                        items(items = state.series, key = { it.id }) { series ->
                            BookSeriesCard(
                                series = series,
                                // Opens the volume's details for now. The reader arrives in Phase 4.
                                onContinueReading = { series.continueBookId?.let(onOpenBook) },
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
                        currentLabel = "Reading: Volume 2",
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
        )
    }
}
