package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun BookSeriesCard(
    title: String,
    author: String,
    illustrator: String,
    totalVolume: Int,
    genres: List<String>,
    onStartReading: () -> Unit,
    onViewSeries: () -> Unit,
    modifier: Modifier = Modifier,
    latestBookCover: String? = null,
    currentVolume: Int = 0,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    var showVolumePreview by rememberSaveable { mutableStateOf(false) }

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
                genres = genres,
                totalVolume = totalVolume
            )

            Details(
                title = title,
                author = author,
                illustrator = illustrator,
                currentVolume = currentVolume,
                totalVolume = totalVolume,
                latestBookCover = latestBookCover
            )

            AnimatedVisibility(visible = showVolumePreview) {
                VolumePreview()
            }

            BottomAction(
                onStartReading = onStartReading,
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
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagChip(
                    text = genres[0],
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = .2f)
                )

                Text(
                    text = "+${genres.size - 1}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            TagChip(
                text = "$totalVolume volumes",
                color = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = { /* TODO: ??? */ }) {
            Icon(
                painter = painterResource(id = R.drawable.more_vert),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun Details(
    title: String,
    author: String,
    illustrator: String,
    currentVolume: Int,
    totalVolume: Int,
    modifier: Modifier = Modifier,
    latestBookCover: String? = null
) {
    val progressPercentage = if (totalVolume > 0) {
        (currentVolume / totalVolume) * 100
    } else {
        0
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BookCover(
            modifier = Modifier.weight(1 / 2f),
            imageUrl = latestBookCover
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    minLines = 2,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "$author, $illustrator",
                    fontWeight = FontWeight.Normal,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    minLines = 1,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
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
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f)
                        )

                        Text(
                            text = "Reading: Volume $currentVolume",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { (progressPercentage / 100).toFloat() },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .4f),
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                        gapSize = 0.dp
                    )

                    Text(
                        text = "$progressPercentage%",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Text(
                text = "$currentVolume of $totalVolume read",
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun BottomAction(
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
            modifier = Modifier.weight(1f)
        ) {
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
                    text = "Continue reading",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        FilledTonalIconButton(onClick = onViewSeries) {
            Icon(
                painter = painterResource(id = R.drawable.list),
                contentDescription = "List",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun VolumePreview(
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Volume sequence",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BookCover()

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Volume 1",
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Text(
                        text = "Finished",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BookCover()

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Volume 2",
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Text(
                        text = "Reading",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BookCover()

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Volume 3",
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Text(
                        text = "Ready",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookSeriesCardPreview() {
    PustakaTheme {
        BookSeriesCard(
            title = "Playing Death Game to Put Food On The Table",
            author = "Yushi Ukai",
            illustrator = "Nekometaru",
            totalVolume = 6,
            genres = listOf("Action", "Adventure"),
            onStartReading = {},
            onViewSeries = {},
            latestBookCover = null,
            currentVolume = 2
        )
    }
}
