package my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main

import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.data.settings.SeriesSort
import java.time.Instant

/**
 * Puts [series] in the order chosen in the Series tab. [series] must already be sorted by name,
 * as the repository returns them, and series that tie stay in that order.
 *
 * @param volumesBySeries each series' volumes, by series id.
 */
internal fun sortSeries(
    series: List<Series>,
    volumesBySeries: Map<Long?, List<Book>>,
    sort: SeriesSort,
): List<Series> = when (sort) {
    SeriesSort.NAME -> series
    // A series counts as added when its newest volume was, or when its folder was found if empty.
    SeriesSort.RECENTLY_ADDED -> series.sortedByDescending { item ->
        volumesBySeries[item.id].orEmpty().maxOfOrNull { it.addedAt } ?: item.createdAt
    }
    SeriesSort.RECENTLY_READ -> series.sortedWith(
        compareByDescending<Series, Instant?>(nullsFirst()) { item ->
            volumesBySeries[item.id].orEmpty().mapNotNull { it.lastOpenedAt }.maxOrNull()
        }
    )
}
