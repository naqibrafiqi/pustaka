package my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.BookSeriesCard
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.components.TopBarActionButton
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun SeriesScreen(
    onNavigateToSeriesDetails: (String) -> Unit
) {
    SeriesScreenContent(
        onNavigateToSeriesDetails = onNavigateToSeriesDetails
    )
}

@Composable
private fun SeriesScreenContent(
    onNavigateToSeriesDetails: (String) -> Unit,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding() + 8.dp),
            state = listState,
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(count = 10) {
                BookSeriesCard(
                    title = "Playing Death Game to Put Food On The Table",
                    author = "Yushi Ukai",
                    illustrator = "Nekometaru",
                    totalVolume = 6,
                    genres = listOf("Action", "Adventure"),
                    onStartReading = {
                        /* TODO: ??? */
                    },
                    onViewSeries = {
                        onNavigateToSeriesDetails("series_uuid7") // TODO: Replace with actual series UUID
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    latestBookCover = null,
                    currentVolume = 2
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SeriesScreenPreview() {
    PustakaTheme {
        SeriesScreenContent(
            onNavigateToSeriesDetails = {}
        )
    }
}
