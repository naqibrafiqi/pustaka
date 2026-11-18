package my.naqibrafiqi.pustaka.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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

/**
 * The same tabs as [BottomBar], down the side of a wide window (D29). It sits clear of the status
 * bar, the navigation bar and a camera cutout on its side.
 */
@Composable
fun SideRail(
    currentItem: BottomBarRoute?,
    items: List<BottomBarRoute>,
    onClick: (Int, BottomBarRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        windowInsets = SideRailInsets,
    ) {
        // Weighted space above and below centres the tabs down the side (#56).
        Spacer(modifier = Modifier.weight(1f))
        items.forEachIndexed { index, item ->
            NavigationRailItem(
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
        Spacer(modifier = Modifier.weight(1f))
    }
}

/** The insets [SideRail] keeps clear of. The screens next to it don't need to keep clear of them too. */
val SideRailInsets: WindowInsets
    @Composable get() = WindowInsets.systemBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)

@Preview(showBackground = true, widthDp = 120, heightDp = 400)
@Composable
private fun SideRailPreview() {
    PustakaTheme {
        SideRail(
            currentItem = BottomBarNavItems[0],
            items = BottomBarNavItems,
            onClick = { _, _ -> }
        )
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
