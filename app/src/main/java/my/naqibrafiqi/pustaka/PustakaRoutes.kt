package my.naqibrafiqi.pustaka

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

interface PustakaRoute : NavKey

// Top-level routes.

@Serializable
data object FilesRoute : PustakaRoute

@Serializable
data object HighlightsRoute : PustakaRoute

@Serializable
data object SettingsRoute : PustakaRoute

// Library routes.

@Serializable
data class BookRoute(val bookId: Int) : PustakaRoute

@Serializable
data class ReaderRoute(val bookId: Int) : PustakaRoute
