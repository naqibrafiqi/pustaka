package my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.BookCover
import my.naqibrafiqi.pustaka.ui.components.GenreChip
import my.naqibrafiqi.pustaka.ui.components.TagChip
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.components.menu.DropdownMenuButton
import my.naqibrafiqi.pustaka.ui.components.menu.MenuItem
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun BookDetailsScreen(
    onNavigateBack: () -> Unit
) {
    BookDetailsScreenContent(
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun BookDetailsScreenContent(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val genres = listOf("Drama", "Fantasy", "Action", "Romance", "Ecchi")

    var openMenu by remember { mutableStateOf(false) }

    var expandSynopsis by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = "Book Details",
                subtitle = "Playing Death Games to Put Food on the Table, Volume 1",
                onNavigateBack = onNavigateBack,
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
                            text = "More info",
                            leadingIconResId = R.drawable.info
                        )

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
                            text = "Delete book",
                            leadingIconResId = R.drawable.delete
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            state = listState,
            contentPadding = PaddingValues(bottom = innerPadding.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BookCover(
                        modifier = Modifier.fillMaxWidth(.5f),
                        imageUrl = null
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Playing Death Games to Put Food on the Table, Volume 1",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "Yushi Ukai, Nekometaru",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = .8f),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (expandSynopsis) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genres.forEach { genre ->
                                GenreChip(text = genre)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Spacer(modifier = Modifier.width(8.dp))

                            genres.forEach { genre ->
                                GenreChip(text = genre)
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            expandSynopsis = !expandSynopsis
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Yuki wakes up to find herself in an unfamiliar manor, wearing a maid uniform and lying on a luxurious bed. Soon, she discovers five more girls, all dressed the same as her. This is the Ghost House, and the only way they can survive is to make it through the traps that lie in wait-deadly games full of blowguns, buzz saws, locked rooms, and weapons. It’s a hopeless, terrifying situation for everyone there…well, everyone except Yuki. After all, this is her career at seventeen. Do you think that’s odd? She would agree. But that’s how some people are-they earn their living playing death games.",
                                fontWeight = FontWeight.Light,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = if (expandSynopsis) Int.MAX_VALUE else 4,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = if (expandSynopsis) "Read Less" else "Read More",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "58%",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleLarge
                                )

                                TagChip(
                                    text = "In Progress",
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            Text(
                                text = "Page 116 of 200",
                                fontWeight = FontWeight.Normal,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        LinearProgressIndicator(
                            progress = { .58f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp),
                            gapSize = 0.dp
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {/* TODO: ??? */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.book_5),
                                    contentDescription = "Continue reading"
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(text = "Continue From Page 116")
                            }
                        }

                        OutlinedButton(
                            onClick = {/* TODO: ??? */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.menu_book),
                                    contentDescription = "Read from beginning"
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(text = "Read From Beginning")
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BasicStatisticsTile(
                            labelText = "Page Count",
                            labelIconRes = R.drawable.article,
                            valueText = "200 pages",
                            modifier = Modifier.weight(1f),
                            labelIconDescription = "Page count"
                        )

                        BasicStatisticsTile(
                            labelText = "Velocity",
                            labelIconRes = R.drawable.speed,
                            valueText = "Avg. 240 wpm",
                            modifier = Modifier.weight(1f),
                            labelIconDescription = "Velocity"
                        )
                    }

                    BasicStatisticsTile(
                        labelText = "Estimated Left",
                        labelIconRes = R.drawable.schedule,
                        valueText = "~2h 15m",
                        modifier = Modifier.fillMaxWidth(),
                        labelIconDescription = "Estimated left"
                    )
                }
            }
        }
    }
}

@Composable
private fun BasicStatisticsTile(
    labelText: String,
    @DrawableRes labelIconRes: Int,
    valueText: String,
    modifier: Modifier = Modifier,
    labelIconDescription: String? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = labelIconRes),
                    contentDescription = labelIconDescription,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )

                Text(
                    text = labelText,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = valueText,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookDetailsScreenPreview() {
    PustakaTheme {
        BookDetailsScreenContent(
            onNavigateBack = {}
        )
    }
}