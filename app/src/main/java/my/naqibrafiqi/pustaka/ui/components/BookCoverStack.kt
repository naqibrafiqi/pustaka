package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun BookCoverStack(
    imageUrls: List<String?>,
    modifier: Modifier = Modifier,
    shiftAmount: Dp = 0.dp,
    borderColor: Color = MaterialTheme.colorScheme.background,
) {
    // 1. Safely limit to 3 items maximum
    val displayCovers = imageUrls.take(3)
    val stackShape = RoundedCornerShape(8.dp)

    Box(
        // By padding the parent Box on the right, all 3 covers measure at the exact
        // same starting size, leaving safe empty space for the background ones to shift into.
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 3. Draw in reverse so index 0 (the front cover) is drawn last and appears on top
        for (index in displayCovers.indices.reversed()) {
            BookCover(
                imageUrl = displayCovers[index],
                shape = stackShape,
                modifier = Modifier
                    .graphicsLayer {
                        // A. Shift the background covers to the right
                        translationX = index * shiftAmount.toPx()

                        // B. Scale them down symmetrically to match your sketch
                        val scale = 1f - (index * 0.12f) // 1.0 (Front), 0.88 (Middle), 0.76 (Back)
                        scaleX = scale
                        scaleY = scale

                        // C. Anchor the scale to the center-left. This ensures the tops and
                        // bottoms shrink inwards, but the left edges stay tucked securely
                        // behind the front cover.
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .border(
                        width = 1.5.dp,
                        color = borderColor,
                        shape = stackShape
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookCoverStackPreview() {
    PustakaTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BookCoverStack(
                imageUrls = listOf(null, null, null, null),
                modifier = Modifier.width(180.dp),
                shiftAmount = 40.dp
            )
        }
    }
}
