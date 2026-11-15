package my.naqibrafiqi.pustaka.ui.feature.settings.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object HiddenItemsRoute : NavKey

@Serializable
data object ReaderDefaultsRoute : NavKey

@Serializable
data object AboutRoute : NavKey

@Serializable
data object LicencesRoute : NavKey

/** One library's licence, by its index in the list of open-source libraries. */
@Serializable
data class LicenceRoute(val libraryIndex: Int) : NavKey
