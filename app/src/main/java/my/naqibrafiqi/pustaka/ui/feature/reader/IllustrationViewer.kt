package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.R
import kotlin.math.max
import kotlin.math.min

/**
 * An illustration full screen over the reader, on black (#50, D35). Pinch or double-tap to zoom in,
 * and drag to move around it. Back or the close button goes back to the page.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IllustrationViewer(illustration: Illustration, onClose: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onClose)
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag(ILLUSTRATION_VIEWER_TAG)
    ) {
        ZoomableIllustration(illustration, Modifier.fillMaxSize())
        IconButton(
            onClick = onClose,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White,
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout))
                .padding(8.dp),
        ) {
            Icon(painter = painterResource(R.drawable.close), contentDescription = stringResource(R.string.common_close))
        }
    }
}

@Composable
private fun ZoomableIllustration(illustration: Illustration, modifier: Modifier = Modifier) {
    val image = remember(illustration) { illustration.bitmap.asImageBitmap() }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val screen = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val fitted = fittedSize(Size(image.width.toFloat(), image.height.toFloat()), screen)
        // Starts again from the whole picture when the screen turns.
        var zoom by remember(illustration, screen) { mutableStateOf(Zoom()) }
        val scope = rememberCoroutineScope()
        var animation by remember { mutableStateOf<Job?>(null) }

        Image(
            bitmap = image,
            contentDescription = illustration.description ?: stringResource(R.string.reader_illustration),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .size(with(density) { fitted.width.toDp() }, with(density) { fitted.height.toDp() })
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offset.x
                    translationY = zoom.offset.y
                }
                // Many books' drawings are black lines on a see-through background, which only
                // show on white, the way the book's own page shows them.
                .background(Color.White),
        )
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(fitted, screen) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        animation?.cancel()
                        zoom = zoom.transformed(centroid - screen.center, pan, gestureZoom, fitted, screen)
                    }
                }
                .pointerInput(fitted, screen) {
                    detectTapGestures(
                        onDoubleTap = { tap ->
                            val from = zoom
                            val to = from.toggled(tap - screen.center, fitted, screen)
                            animation?.cancel()
                            animation = scope.launch {
                                animate(0f, 1f) { progress, _ ->
                                    zoom = Zoom(lerp(from.scale, to.scale, progress), lerp(from.offset, to.offset, progress))
                                }
                            }
                        },
                    )
                },
        )
    }
}

private val Size.center: Offset get() = Offset(width / 2, height / 2)

/** The size of [picture] made as large as fits in [screen], keeping its shape. */
internal fun fittedSize(picture: Size, screen: Size): Size {
    if (picture.width <= 0f || picture.height <= 0f) return Size.Zero
    val scale = min(screen.width / picture.width, screen.height / picture.height)
    return Size(picture.width * scale, picture.height * scale)
}

/**
 * How far the picture is zoomed in, and how far it's moved from the middle of the screen. The
 * picture starts fitted to the screen, at a [scale] of 1.
 */
internal data class Zoom(val scale: Float = 1f, val offset: Offset = Offset.Zero) {

    /**
     * After a pinch or drag: zoomed by [zoomBy] around [centroid] (from the middle of the screen)
     * and moved by [pan], without zooming out past the whole picture or moving it off the screen.
     *
     * @param picture the picture's size before zooming, fitted to [screen].
     */
    fun transformed(centroid: Offset, pan: Offset, zoomBy: Float, picture: Size, screen: Size): Zoom {
        val newScale = (scale * zoomBy).coerceIn(MIN_SCALE, MAX_SCALE)
        // The point under the fingers stays under them.
        val newOffset = centroid - (centroid - offset) * (newScale / scale) + pan
        return Zoom(newScale, clamp(newOffset, newScale, picture, screen))
    }

    /**
     * After a double tap: back to the whole picture when zoomed in. Otherwise zoomed in around
     * [tap] (from the middle of the screen), far enough to fill the screen, and at least twice.
     */
    fun toggled(tap: Offset, picture: Size, screen: Size): Zoom {
        if (scale > MIN_SCALE + ZOOMED_IN) return Zoom()
        val fill = if (picture.width > 0f && picture.height > 0f) max(screen.width / picture.width, screen.height / picture.height) else 1f
        val newScale = max(DOUBLE_TAP_SCALE, fill).coerceAtMost(MAX_SCALE)
        return Zoom(newScale, clamp(tap - tap * newScale, newScale, picture, screen))
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 6f
        const val DOUBLE_TAP_SCALE = 2f

        /** Less than this past the whole picture counts as not zoomed in. */
        private const val ZOOMED_IN = 0.01f

        /** Keeps the picture's edges from coming inside the screen's, or centred when it's smaller. */
        fun clamp(offset: Offset, scale: Float, picture: Size, screen: Size): Offset {
            val maxX = max(0f, (picture.width * scale - screen.width) / 2)
            val maxY = max(0f, (picture.height * scale - screen.height) / 2)
            return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
        }
    }
}

const val ILLUSTRATION_VIEWER_TAG = "illustrationViewer"
