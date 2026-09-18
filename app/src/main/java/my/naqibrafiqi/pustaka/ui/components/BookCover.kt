package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

@Composable
fun BookCover(
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    coverRatio: Float = 2f / 3f,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    AsyncImage(
        model = imageUrl,
        contentDescription = "Book cover",
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(coverRatio)
            .clip(shape),
        placeholder = painterResource(id = R.drawable.placeholder),
        error = painterResource(id = R.drawable.placeholder),
        fallback = painterResource(id = R.drawable.placeholder),
        contentScale = ContentScale.Crop
    )
}

@Preview(showBackground = true)
@Composable
private fun BookCoverPreview() {
    PustakaTheme {
        BookCover()
    }
}
