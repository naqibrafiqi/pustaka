package my.naqibrafiqi.pustaka.ui.feature.series.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.ui.feature.series.api.SeriesRoute
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.SeriesScreen

fun EntryProviderScope<NavKey>.seriesEntryBuilder() {
    entry<SeriesRoute> {
        SeriesScreen()
    }
}
