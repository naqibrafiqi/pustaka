package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.model.BookState
import my.naqibrafiqi.pustaka.ui.model.SeriesItem
import my.naqibrafiqi.pustaka.ui.model.VolumePreviewItem
import my.naqibrafiqi.pustaka.ui.model.volumesLabel
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun BookSeriesCard(
    series: SeriesItem,
    onContinueReading: () -> Unit,
    onViewSeries: () -> Unit,
    onOpenVolume: (Long) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    var showVolumePreview by rememberSaveable(series.id) { mutableStateOf(false) }

    Card(
        onClick = {
            showVolumePreview = !showVolumePreview
        },
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopAction(
                genres = series.genres,
                totalVolume = series.volumeCount
            )

            Details(series = series)

            AnimatedVisibility(visible = showVolumePreview) {
                VolumePreview(
                    volumes = series.preview,
                    onOpenVolume = onOpenVolume
                )
            }

            BottomAction(
                canContinue = series.continueBookId != null,
                onStartReading = onContinueReading,
                onViewSeries = onViewSeries
            )
        }
    }
}

@Composable
private fun TopAction(
    genres: List<String>,
    totalVolume: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (genres.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagChip(
                    text = genres[0],
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = .2f)
                )

                if (genres.size > 1) {
                    Text(
                        text = "+${genres.size - 1}",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        TagChip(
            text = volumesLabel(totalVolume),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun Details(
    series: SeriesItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BookCover(
            modifier = Modifier.weight(1 / 2f),
            imageUrl = series.coverPath
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = series.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    minLines = 2,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = series.credits ?: "Unknown author",
                    fontWeight = FontWeight.Normal,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    minLines = 1,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.book_5),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f)
                    )

                    Text(
                        text = series.currentLabel,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { series.percent / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .4f),
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                        gapSize = 0.dp
                    )

                    Text(
                        text = "${series.percent}%",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Text(
                text = "${series.finishedCount} of ${series.volumeCount} read",
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun BottomAction(
    canContinue: Boolean,
    onStartReading: () -> Unit,
    onViewSeries: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onStartReading,
            enabled = canContinue,
            modifier = Modifier.weight(1f)
        ) {
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
                    text = "Continue reading",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        FilledTonalIconButton(onClick = onViewSeries) {
            Icon(
                painter = painterResource(id = R.drawable.list),
                contentDescription = "View series",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun VolumePreview(
    volumes: List<VolumePreviewItem>,
    onOpenVolume: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = Dp.Hairline
        )

        Text(
            text = "Volume sequence",
            style = MaterialTheme.typography.labelLarge
        )

        if (volumes.isEmpty()) {
            Text(
                text = "This folder has no EPUB files yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            volumes.forEach { volume ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenVolume(volume.bookId) }
                        .alpha(if (volume.state == BookState.MISSING) .5f else 1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BookCover(imageUrl = volume.coverPath)

                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = volume.label,
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.labelLarge
                        )

                        Text(
                            text = volume.state.label,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Keeps covers the same size when there are fewer than three volumes.
            repeat(3 - volumes.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookSeriesCardPreview() {
    PustakaTheme {
        BookSeriesCard(
            series = SeriesItem(
                id = 1,
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
                preview = listOf(
                    VolumePreviewItem(1, "Volume 1", null, BookState.FINISHED),
                    VolumePreviewItem(2, "Volume 2", null, BookState.READING),
                    VolumePreviewItem(3, "Volume 3", null, BookState.UNREAD),
                ),
            ),
            onContinueReading = {},
            onViewSeries = {},
            onOpenVolume = {},
        )
    }
}
