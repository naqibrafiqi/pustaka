package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(100),
    color: Color = MaterialTheme.colorScheme.surface
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        tonalElevation = 32.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 4.dp
            )
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagChipPreview() {
    PustakaTheme {
        TagChip(
            text = "Sci-fi",
            color = MaterialTheme.colorScheme.primary.copy(alpha = .6f)
        )
    }
}
