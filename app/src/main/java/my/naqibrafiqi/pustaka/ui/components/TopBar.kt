package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R

@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    actionButtons: @Composable (() -> Unit)? = null,
    tonalElevation: Dp = 0.dp
) {
    Surface(
        modifier = modifier,
        tonalElevation = tonalElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    top = 8.dp,
                    end = 8.dp,
                    bottom = 8.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "Navigate back"
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                repeatDelayMillis = 1000,
                                initialDelayMillis = 1000,
                                spacing = MarqueeSpacing(spacing = 24.dp),
                                velocity = (-30).dp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f),
                            overflow = TextOverflow.Visible,
                            maxLines = 1,
                            minLines = 1,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (actionButtons != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actionButtons.invoke()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TopBarPreview() {
    TopBar(
        title = "Title",
        subtitle = "Subtitle",
        onNavigateBack = {}
    )
}
