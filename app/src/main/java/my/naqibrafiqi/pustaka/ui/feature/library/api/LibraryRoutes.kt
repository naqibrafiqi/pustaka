package my.naqibrafiqi.pustaka.ui.feature.library.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import my.naqibrafiqi.pustaka.PustakaRoute

@Serializable
data object LibraryRoute : PustakaRoute
