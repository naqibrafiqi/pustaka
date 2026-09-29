package my.naqibrafiqi.pustaka.ui.components.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R

@Composable
fun DropdownMenuButton(
    expanded: Boolean,
    openMenu: () -> Unit,
    closeMenu: () -> Unit,
    modifier: Modifier = Modifier,
    menuItemContent: @Composable (() -> Unit)
) {
    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = openMenu) {
            Icon(
                painter = painterResource(id = R.drawable.more_vert),
                contentDescription = "More actions dropdown menu"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = closeMenu,
            modifier = Modifier.padding(vertical = 8.dp),
            offset = DpOffset(x = (-8).dp, y = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            menuItemContent.invoke()
        }
    }
}
