package my.naqibrafiqi.pustaka.util.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import kotlinx.serialization.serializer
import my.naqibrafiqi.pustaka.PustakaRoute

@Composable
fun rememberPustakaNavBackStack(vararg elements: PustakaRoute): NavBackStack<PustakaRoute> {
    return rememberSerializable(serializer = serializer()) {
        NavBackStack(*elements)
    }
}
