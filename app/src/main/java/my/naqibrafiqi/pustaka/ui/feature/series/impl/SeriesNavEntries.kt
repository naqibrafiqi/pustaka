package my.naqibrafiqi.pustaka.ui.feature.series.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.ui.feature.series.api.SeriesDetailsRoute
import my.naqibrafiqi.pustaka.ui.feature.series.api.SeriesRoute
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDetailsScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.SeriesScreen
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator

fun EntryProviderScope<NavKey>.seriesEntryBuilder() {
    entry<SeriesRoute> {
        val navigator = LocalAppNavigator.current

        SeriesScreen(
            onNavigateToSeriesDetails = { seriesId ->
                navigator.add(SeriesDetailsRoute(seriesId))
            }
        )
    }

    entry<SeriesDetailsRoute> {
        val navigator = LocalAppNavigator.current

        SeriesDetailsScreen()
    }
}
