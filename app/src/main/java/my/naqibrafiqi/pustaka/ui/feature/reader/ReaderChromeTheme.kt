package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The colours of the reader's bars and sheets. With the Dark grey theme they're dark, with grey
 * surfaces that go with the page, whatever the app's own theme is (#55). Otherwise they're the
 * app's own colours, as before.
 *
 * Must be inside a dark [MaterialTheme] when [darkGrey] is true, so the accent colours are dark ones.
 */
@Composable
fun ReaderChromeTheme(darkGrey: Boolean, content: @Composable () -> Unit) {
    // Always wrapped the same way, so switching themes doesn't rebuild the reader underneath.
    val colorScheme = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme = if (darkGrey) colorScheme.withGreySurfaces() else colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

/** These colours with neutral grey surfaces around the Dark grey page's colour. */
fun ColorScheme.withGreySurfaces(): ColorScheme = copy(
    background = Color(DARK_GREY_BACKGROUND),
    onBackground = GreyOnSurface,
    surface = Color(DARK_GREY_BACKGROUND),
    onSurface = GreyOnSurface,
    surfaceVariant = Color(0xFF474747),
    onSurfaceVariant = Color(0xFFC4C4C4),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFF262626),
    surfaceBright = Color(0xFF4A4A4A),
    surfaceContainerLowest = Color(0xFF262626),
    surfaceContainerLow = Color(0xFF363636),
    surfaceContainer = Color(0xFF3A3A3A),
    surfaceContainerHigh = Color(0xFF444444),
    surfaceContainerHighest = Color(0xFF4E4E4E),
    inverseSurface = GreyOnSurface,
    inverseOnSurface = Color(DARK_GREY_BACKGROUND),
    outline = Color(0xFF8E8E8E),
    outlineVariant = Color(0xFF5A5A5A),
)

private val GreyOnSurface = Color(0xFFE3E3E3)
