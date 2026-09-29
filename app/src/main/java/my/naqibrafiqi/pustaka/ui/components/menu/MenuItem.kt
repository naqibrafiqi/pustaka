package my.naqibrafiqi.pustaka.ui.components.menu

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

@Composable
fun MenuItem(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    leadingIconResId: Int? = null,
    trailingIconResId: Int? = null,
    leadingIconColor: Color = MenuDefaults.itemColors().leadingIconColor,
    trailingIconColor: Color = MenuDefaults.itemColors().trailingIconColor,
    enabled: Boolean = true
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = if (leadingIconResId != null) {
            {
                Icon(
                    painter = painterResource(id = leadingIconResId),
                    contentDescription = null,
                    tint = leadingIconColor
                )
            }
        } else {
            null
        },
        trailingIcon = if (trailingIconResId != null) {
            {
                Icon(
                    painter = painterResource(id = trailingIconResId),
                    contentDescription = null,
                    tint = trailingIconColor
                )
            }
        } else {
            null
        },
        enabled = enabled
    )
}
