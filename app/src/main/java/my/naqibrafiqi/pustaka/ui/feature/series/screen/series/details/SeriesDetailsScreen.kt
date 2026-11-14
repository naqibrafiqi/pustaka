package my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.BookCard
import my.naqibrafiqi.pustaka.ui.components.BookCoverFan
import my.naqibrafiqi.pustaka.ui.components.ErrorState
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.components.SynopsisSection
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.components.menu.DropdownMenuButton
import my.naqibrafiqi.pustaka.ui.components.menu.MenuItem
import my.naqibrafiqi.pustaka.ui.model.BookItem
import my.naqibrafiqi.pustaka.ui.model.BookState
import my.naqibrafiqi.pustaka.ui.model.volumesLabel
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun SeriesDetailsScreen(
    state: SeriesDetailsUiState,
    onNavigateBack: () -> Unit,
    onToggleLayout: () -> Unit,
    onRetry: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    SeriesDetailsScreenContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onToggleLayout = onToggleLayout,
        onRetry = onRetry,
        onOpenBook = onOpenBook,
        onEdit = onEdit,
        onDelete = onDelete,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun SeriesDetailsScreenContent(
    state: SeriesDetailsUiState,
    onNavigateBack: () -> Unit,
    onToggleLayout: () -> Unit,
    onRetry: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val ready = state as? SeriesDetailsUiState.Ready
    val isGrid = ready?.isGrid ?: true

    var openMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBar(
                title = "Series Details",
                subtitle = ready?.name,
                onNavigateBack = onNavigateBack,
                actionButtons = if (ready == null) null else {
                    {
                        DropdownMenuButton(
                            expanded = openMenu,
                            openMenu = {
                                openMenu = true
                            },
                            closeMenu = {
                                openMenu = false
                            }
                        ) {
                            MenuItem(
                                onClick = {
                                    openMenu = false
                                    onEdit()
                                },
                                text = "Edit details",
                                leadingIconResId = R.drawable.edit
                            )

                            MenuItem(
                                onClick = {
                                    openMenu = false
                                    onDelete()
                                },
                                text = "Delete series",
                                leadingIconResId = R.drawable.delete
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())

        when (state) {
            SeriesDetailsUiState.Loading -> LoadingState(modifier = contentModifier)
            is SeriesDetailsUiState.Error -> ErrorState(
                message = state.message,
                onRetry = onRetry,
                modifier = contentModifier,
            )
            SeriesDetailsUiState.NotFound -> MessageState(
                title = "Series not found",
                message = "This series is no longer in your library.",
                iconRes = R.drawable.newsstand,
                modifier = contentModifier,
                actionLabel = "Go back",
                onAction = onNavigateBack,
            )
            is SeriesDetailsUiState.Ready -> LazyVerticalGrid(
                columns = if (isGrid) GridCells.Fixed(2) else GridCells.Fixed(1),
                modifier = contentModifier,
                state = gridState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                readyContent(state, onToggleLayout, onOpenBook)
            }
        }
    }
}

private fun LazyGridScope.readyContent(
    state: SeriesDetailsUiState.Ready,
    onToggleLayout: () -> Unit,
    onOpenBook: (Long) -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Header(
            title = state.name,
            author = state.author,
            illustrator = state.illustrator,
            totalVolume = state.volumeCount,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            volumeCovers = state.headerCovers
        )
    }

    if (state.genres.isNotEmpty() || !state.synopsis.isNullOrBlank()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            // The grid already pads the sides, so the section adds none of its own.
            SynopsisSection(
                genres = state.genres,
                synopsis = state.synopsis,
                horizontalPadding = 0.dp,
            )
        }
    }

    item(span = { GridItemSpan(maxLineSpan) }) {
        ReadingStatistics(
            totalVolume = state.volumeCount,
            finishedVolume = state.finishedCount,
            percent = state.percent,
            folderPath = state.folderPath,
            totalSize = state.totalSize,
            indexedCount = state.indexedCount,
            modifier = Modifier.fillMaxWidth()
        )
    }

    stickyHeader {
        VolumesHeader(
            volumeCount = state.volumeCount,
            isGrid = state.isGrid,
            onChangeLayout = onToggleLayout
        )
    }

    if (state.volumes.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            MessageState(
                title = "No volumes yet",
                message = "Copy this series' EPUB files into ${state.folderPath}, then pull down to " +
                    "refresh in Library or Series.",
                iconRes = R.drawable.book_5,
            )
        }
    } else {
        items(items = state.volumes, key = { it.id }) { volume ->
            BookCard(
                book = volume,
                onClick = { onOpenBook(volume.id) },
                modifier = Modifier.fillMaxWidth(1f),
                isGrid = state.isGrid,
            )
        }
    }
}

@Composable
private fun Header(
    title: String,
    author: String?,
    illustrator: String?,
    totalVolume: Int,
    modifier: Modifier = Modifier,
    volumeCovers: List<String?> = emptyList()
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.Top
    ) {
        BookCoverFan(
            imageUrls = volumeCovers.ifEmpty { listOf(null) },
            modifier = Modifier.weight(.4f)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                author?.let {
                    DetailsWithIcon(
                        text = it,
                        iconRes = R.drawable.stylus_fountain_pen
                    )
                }

                illustrator?.let {
                    DetailsWithIcon(
                        text = it,
                        iconRes = R.drawable.stylus_brush
                    )
                }

                DetailsWithIcon(
                    text = volumesLabel(totalVolume),
                    iconRes = R.drawable.book_5
                )
            }
        }
    }
}

@Composable
fun DetailsWithIcon(
    text: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = text,
            fontWeight = FontWeight.Normal,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ReadingStatistics(
    totalVolume: Int,
    finishedVolume: Int,
    percent: Int,
    folderPath: String,
    totalSize: String,
    indexedCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        painter = painterResource(id = R.drawable.donut_large),
                        contentDescription = null,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(100)
                            )
                            .padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Column {
                        Text(
                            text = "Series Progress",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "$finishedVolume of $totalVolume volumes read",
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Text(
                    text = "$percent%",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                trackColor = MaterialTheme.colorScheme.tertiaryContainer,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                gapSize = 0.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceDim,
                        shape = RoundedCornerShape(100)
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.storage),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )

                Column {
                    Text(
                        text = folderPath,
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium
                    )

                    Text(
                        text = "$totalSize · $indexedCount of $totalVolume files indexed",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumesHeader(
    volumeCount: Int,
    isGrid: Boolean,
    onChangeLayout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Volumes in Sequence",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = volumesLabel(volumeCount),
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.labelMedium
            )
        }

        IconButton(onClick = onChangeLayout) {
            Icon(
                painter = painterResource(id = if (isGrid) R.drawable.list else R.drawable.grid),
                contentDescription = if (isGrid) "Show as list" else "Show as grid"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SeriesDetailsScreenPreview() {
    PustakaTheme {
        SeriesDetailsScreenContent(
            state = SeriesDetailsUiState.Ready(
                name = "Playing Death Games to Put Food on the Table",
                author = "Yushi Ukai",
                illustrator = "Nekometaru",
                synopsis = "Yuki wakes up to find herself in an unfamiliar manor.",
                genres = listOf("Action", "Thriller"),
                headerCovers = listOf(null, null, null),
                volumeCount = 3,
                finishedCount = 1,
                percent = 33,
                folderPath = "/storage/emulated/0/Pustaka/Playing Death Games",
                totalSize = "14.2 MB",
                indexedCount = 3,
                volumes = List(3) { index ->
                    BookItem(
                        id = index.toLong(),
                        title = "Playing Death Games, Vol. 0${index + 1}",
                        credits = "Yushi Ukai",
                        coverPath = null,
                        state = if (index == 0) BookState.FINISHED else BookState.UNREAD,
                        percent = if (index == 0) 100 else 0,
                        volumeLabel = "Volume ${index + 1}",
                    )
                },
                isGrid = true,
            ),
            onNavigateBack = {},
            onToggleLayout = {},
            onRetry = {},
            onOpenBook = {},
            onEdit = {},
            onDelete = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
