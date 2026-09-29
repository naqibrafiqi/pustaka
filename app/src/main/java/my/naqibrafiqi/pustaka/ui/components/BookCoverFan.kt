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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun BookCoverFan(
    imageUrls: List<String?>,
    modifier: Modifier = Modifier,
    rotation: Float = 8f
) {
    val displayCovers = imageUrls.take(3)
    val stackShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Draw in reverse so index 0 (front cover) is on top
        for (index in displayCovers.indices.reversed()) {

            BookCover(
                imageUrl = displayCovers[index],
                shape = stackShape,
                modifier = Modifier
                    .graphicsLayer {
                        // 1. Anchor all transformations to the EXACT bottom-right corner
                        transformOrigin = TransformOrigin(1f, 1f)

                        // 2. Scale them down proportionally (1.0, 0.88, 0.76)
                        // Because of the anchor, they shrink downwards and to the right.
                        val scale = 1f - (index * 0.12f)
                        scaleX = scale
                        scaleY = scale

                        // 3. Rotate clockwise to swing the top-right corners outward
                        rotationZ = index * rotation
                    }
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.background,
                        shape = stackShape
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookCoverFanPreview() {
    PustakaTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BookCoverFan(
                imageUrls = listOf(null, null, null, null, null),
                modifier = Modifier.width(180.dp)
            )
        }
    }
}
