package my.naqibrafiqi.pustaka.ui.feature.series.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SeriesRoute : NavKey

@Serializable
data class SeriesDetailsRoute(val seriesId: String) : NavKey
