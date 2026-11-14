package my.naqibrafiqi.pustaka.ui.feature.library.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridFlow
import androidx.compose.foundation.layout.GridTrackSize
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import my.naqibrafiqi.pustaka.ui.components.SelectChip
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.components.TopBarActionButton
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun LibraryScreen(
    onNavigateToBookDetail: (String) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    LibraryScreenContent(
        onNavigateToBookDetail = onNavigateToBookDetail,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    )
}

@OptIn(ExperimentalGridApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreenContent(
    onNavigateToBookDetail: (String) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = "Library",
                actionButtons = {
                    TopBarActionButton(
                        onClick = { /* TODO: ??? */ },
                        iconResId = R.drawable.search
                    )

                    TopBarActionButton(
                        onClick = { /* TODO: ??? */ },
                        iconResId = R.drawable.grid
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
            ) {
                stickyHeader {
                    FilterChipsScrollableRow(
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.background)
                            .padding(
                                top = 8.dp,
                                bottom = 16.dp
                            )
                    )
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

                            Text(
                                text = "Read 32m ago",
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = .8f),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        CurrentlyReadingCard()
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
                                bottom = 8.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Added",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = "(24 books)",
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = .8f),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        TextButton(onClick = { /* TODO: ??? */ }) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "View all",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Icon(
                                    painter = painterResource(id = R.drawable.chevron_forward),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

                item {
                    Grid(
                        config = {
                            repeat(2) {
                                column(GridTrackSize.MinMax(min = 0.dp, max = 1.fr))
                            }
                            gap(16.dp)
                            flow = GridFlow.Row
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        repeat(25) {
                            BookCard(
                                onClick = { onNavigateToBookDetail("bookId") },
                                title = "The Left Hand of Darkness",
                                subtitle = "Ursula K. Le Guin",
                                progress = .42f
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsScrollableRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier)

        SelectChip(
            selected = true,
            onClick = { /* TODO: ??? */ },
            label = "All books (24)"
        )

        SelectChip(
            selected = false,
            onClick = { /* TODO: ??? */ },
            label = "Currently reading (2)"
        )

        SelectChip(
            selected = false,
            onClick = { /* TODO: ??? */ },
            label = "Unread (1)"
        )

        Spacer(modifier = Modifier)
    }
}

@Composable
private fun CurrentlyReadingCard(
    modifier: Modifier = Modifier
) {
    Card(
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
                BookCover()
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "My Book Title",
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Author Name • Illustrator Name",
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.labelLarge
                )
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
                        Icon(
                            painter = painterResource(id = R.drawable.book_5),
                            contentDescription = "Book chapter",
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            text = "Chapter 14",
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Text(
                        text = "68% complete",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                LinearProgressIndicator(
                    progress = { 0.68f },
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
                    text = "Page 214 of 312",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )

                Button(onClick = { /* TODO: ??? */ }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.play_arrow),
                            contentDescription = "Play",
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

@Preview(showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    PustakaTheme {
        LibraryScreenContent(
            onNavigateToBookDetail = {},
            isRefreshing = false,
            onRefresh = {}
        )
    }
}
