package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun TopBarActionButton(
    onClick: () -> Unit,
    iconResId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TopBarActionButtonPreview() {
    PustakaTheme {
        TopBarActionButton(
            onClick = {},
            iconResId = R.drawable.grid
        )
    }
}
