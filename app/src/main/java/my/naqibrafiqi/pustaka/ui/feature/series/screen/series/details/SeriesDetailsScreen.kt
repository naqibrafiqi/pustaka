package my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.BookCard
import my.naqibrafiqi.pustaka.ui.components.BookCoverFan
import my.naqibrafiqi.pustaka.ui.components.TagChip
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.components.menu.DropdownMenuButton
import my.naqibrafiqi.pustaka.ui.components.menu.MenuItem
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SeriesDetailsScreen() {
    SeriesDetailsScreenContent(
        // TODO: Pass series details parameters
    )
}

@Composable
private fun SeriesDetailsScreenContent(
    modifier: Modifier = Modifier
) {
    val tags = listOf("Action", "Battle royale", "Drama", "Mystery", "Psychological", "Thriller")

    var openMenu by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

    var isGrid by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = "Series Details",
                subtitle = "Playing Death Games to Put Food on the Table",
                onNavigateBack = { /* TODO: ??? */ },
                actionButtons = {
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
                                /* TODO: ??? */
                            },
                            text = "Edit details",
                            leadingIconResId = R.drawable.edit
                        )

                        MenuItem(
                            onClick = {
                                openMenu = false
                                /* TODO: ??? */
                            },
                            text = "Delete series",
                            leadingIconResId = R.drawable.delete
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = if (isGrid) GridCells.Fixed(2) else GridCells.Fixed(1),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            state = gridState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateTopPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Header(
                    title = "Playing Death Games to Put Food on the Table",
                    author = "Yushi Ukai",
                    illustrator = "Nekometaru",
                    mainGenre = "Action and Adventure",
                    totalVolume = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    volumeCovers = listOf(null, null, null, null, null, null)
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SynopsisAndGenres(
                    tags = tags,
                    synopsis = "Yuki wakes up to find herself in an unfamiliar manor, wearing a maid uniform and lying on a luxurious bed. Soon, she discovers five more girls, all dressed the same as her. This is the Ghost House, and the only way they can survive is to make it through the traps that lie in wait-deadly games full of blowguns, buzz saws, locked rooms, and weapons. It’s a hopeless, terrifying situation for everyone there…well, everyone except Yuki. After all, this is her career at seventeen. Do you think that’s odd? She would agree. But that’s how some people are-they earn their living playing death games.",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ReadingStatistics(
                    totalVolume = 6,
                    currentVolume = 2,
                    currentVolumeProgress = 68,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Storage(
                    totalFiles = 10,
                    totalSyncedFiles = 6,
                    storageUri = "/storage/emulated/0/Pustaka/01a0b844-afbc-75b6-969a-f446fedefb8a",
                    totalFilesSize = 14.2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            stickyHeader {
                VolumesHeader(
                    onChangeLayout = {
                        isGrid = !isGrid
                    }
                )
            }

            items(count = 6) { index ->
                BookCard(
                    onClick = { /* TODO: ??? */ },
                    title = "Volume ${index + 1}",
                    modifier = Modifier.fillMaxWidth(1f),
                    isGrid = isGrid,
                    imageUrl = null,
                    progress = 0f
                )
            }
        }
    }
}

@Composable
private fun Header(
    title: String,
    author: String,
    illustrator: String,
    mainGenre: String,
    totalVolume: Int,
    modifier: Modifier = Modifier,
    volumeCovers: List<String?> = emptyList()
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BookCoverFan(
            imageUrls = volumeCovers.ifEmpty { listOf(null) },
            modifier = Modifier.width(160.dp)
        )

        TagChip(
            text = "$totalVolume " + pluralStringResource(
                id = R.plurals.common_label_volume,
                count = totalVolume
            ) + " • $mainGenre",
            color = MaterialTheme.colorScheme.primaryContainer
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Normal
                        )
                    ) {
                        append(author)
                    }

                    append(", ")

                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Normal
                        )
                    ) {
                        append(illustrator)
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun SynopsisAndGenres(
    tags: List<String>,
    synopsis: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "ArrowRotation"
    )

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Genre tags
        if (expanded) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    TagChip(text = tag)
                }
            }
        } else {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        state = rememberScrollState(),
                        enabled = true
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    TagChip(text = tag)
                }
            }
        }

        // Synopsis card
        Surface(
            onClick = {
                expanded = !expanded

                if (expanded) {
                    coroutineScope.launch {
                        delay(150.milliseconds)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = synopsis,
                    fontWeight = FontWeight.Light,
                    lineHeight = 20.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) {
                            stringResource(id = R.string.common_label_read_less)
                        } else {
                            stringResource(id = R.string.common_label_read_more)
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.keyboard_arrow_down),
                        contentDescription = if (expanded) "Collapse synopsis" else "Expand synopsis",
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(iconRotation),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingStatistics(
    totalVolume: Int,
    currentVolume: Int,
    currentVolumeProgress: Int,
    modifier: Modifier = Modifier
) {
    val volumePercentage = (currentVolume.toFloat() / totalVolume.toFloat()) * 100

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
                            text = "Series Reading Progress",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "$currentVolume of $totalVolume volumes read",
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Text(
                    text = "${volumePercentage.toInt()}%",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            LinearProgressIndicator(
                progress = { (volumePercentage / 100) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.book_5),
                        contentDescription = "Book chapter",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )

                    Text(
                        text = "Volume $currentVolume • Reading",
                        fontWeight = FontWeight.Medium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Text(
                    text = "$currentVolumeProgress% complete",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun Storage(
    totalFiles: Int,
    totalSyncedFiles: Int,
    storageUri: String,
    totalFilesSize: Double,
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
                        painter = painterResource(id = R.drawable.folder_managed),
                        contentDescription = null,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(100)
                            )
                            .padding(8.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Column {
                        Text(
                            text = "Offline Storage",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "$totalSyncedFiles of $totalFiles files synced",
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.storage),
                        contentDescription = "Book chapter",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )

                    Column {
                        Text(
                            text = storageUri,
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium
                        )

                        Text(
                            text = "$totalFilesSize MB",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumesHeader(
    onChangeLayout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    text = "6 Titles Indexed Locally",
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            IconButton(onClick = onChangeLayout) {
                Icon(
                    painter = painterResource(id = R.drawable.grid),
                    contentDescription = "Grid list"
                )
            }
        }


    }
}

@Preview(showBackground = true)
@Composable
private fun SeriesDetailsScreenPreview() {
    PustakaTheme {
        SeriesDetailsScreenContent()
    }
}
