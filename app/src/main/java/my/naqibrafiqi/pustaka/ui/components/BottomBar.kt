package my.naqibrafiqi.pustaka.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.feature.highlights.api.HighlightsRoute
import my.naqibrafiqi.pustaka.ui.feature.library.api.LibraryRoute
import my.naqibrafiqi.pustaka.ui.feature.series.api.SeriesRoute
import my.naqibrafiqi.pustaka.ui.feature.settings.api.SettingsRoute
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

data class BottomBarRoute(
    val route: NavKey,
    @get:StringRes val label: Int,
    @get:DrawableRes val icon: Int
)

val BottomBarNavItems = listOf(
    BottomBarRoute(
        route = LibraryRoute,
        label = R.string.navigation_label_library,
        icon = R.drawable.local_library
    ),
    BottomBarRoute(
        route = SeriesRoute,
        label = R.string.navigation_label_series,
        icon = R.drawable.newsstand
    ),
    BottomBarRoute(
        route = HighlightsRoute,
        label = R.string.navigation_label_highlights,
        icon = R.drawable.bookmark_stacks
    ),
    BottomBarRoute(
        route = SettingsRoute,
        label = R.string.navigation_label_settings,
        icon = R.drawable.settings
    )
)

@Composable
fun BottomBar(
    currentItem: BottomBarRoute?,
    items: List<BottomBarRoute>,
    onClick: (Int, BottomBarRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = currentItem?.let { it == item } ?: (index == 0),
                onClick = {
                    onClick(index, item)
                },
                icon = {
                    Icon(
                        painterResource(id = item.icon),
                        contentDescription = null
                    )
                },
                label = {
                    Text(text = stringResource(id = item.label))
                },
                alwaysShowLabel = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomBarPreview() {
    PustakaTheme {
        BottomBar(
            currentItem = BottomBarNavItems[0],
            items = BottomBarNavItems,
            onClick = { _, _ -> }
        )
    }
}
