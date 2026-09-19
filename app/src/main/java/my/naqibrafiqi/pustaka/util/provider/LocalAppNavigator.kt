package my.naqibrafiqi.pustaka.util.provider

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

val LocalAppNavigator = staticCompositionLocalOf<NavBackStack<NavKey>> {
    error("No back stack provided")
}
