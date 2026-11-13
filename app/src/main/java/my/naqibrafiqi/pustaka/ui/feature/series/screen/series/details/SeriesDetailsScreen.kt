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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.components.menu.DropdownMenuButton
import my.naqibrafiqi.pustaka.ui.components.menu.MenuItem
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Header(
                    title = "Playing Death Games to Put Food on the Table",
                    author = "Yushi Ukai",
                    illustrator = "Nekometaru",
                    totalVolume = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    volumeCovers = listOf(null, null, null)
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ReadingStatistics(
                    totalVolume = 6,
                    currentVolume = 2,
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
                DetailsWithIcon(
                    text = author,
                    iconRes = R.drawable.stylus_fountain_pen
                )

                DetailsWithIcon(
                    text = illustrator,
                    iconRes = R.drawable.stylus_brush
                )

                DetailsWithIcon(
                    text = "$totalVolume volumes",
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
    currentVolume: Int,
    storageUri: String,
    totalFilesSize: Double,
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
                            text = "Series Progress",
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
