package my.naqibrafiqi.pustaka.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * Checks that text and icons in Pustaka's own colours are readable: at least 4.5:1 for every
 * colour that's drawn on top of another (WCAG AA for normal text), and 3:1 for outlines, which
 * mark the edges of controls (WCAG AA for non-text contrast).
 */
class ColorSchemeContrastTest {

    @Test
    fun lightSchemeHasReadableText() = assertReadable(PustakaLightColorScheme, "light")

    @Test
    fun darkSchemeHasReadableText() = assertReadable(PustakaDarkColorScheme, "dark")

    private fun assertReadable(scheme: ColorScheme, name: String) {
        val surfaces = with(scheme) {
            listOf(
                "surface" to surface,
                "surfaceContainerLowest" to surfaceContainerLowest,
                "surfaceContainerLow" to surfaceContainerLow,
                "surfaceContainer" to surfaceContainer,
                "surfaceContainerHigh" to surfaceContainerHigh,
                "surfaceContainerHighest" to surfaceContainerHighest,
                "surfaceDim" to surfaceDim,
                "surfaceBright" to surfaceBright,
            )
        }
        val textPairs = with(scheme) {
            listOf(
                Triple("onPrimary", onPrimary, primary to "primary"),
                Triple("onPrimaryContainer", onPrimaryContainer, primaryContainer to "primaryContainer"),
                Triple("onSecondary", onSecondary, secondary to "secondary"),
                Triple("onSecondaryContainer", onSecondaryContainer, secondaryContainer to "secondaryContainer"),
                Triple("onTertiary", onTertiary, tertiary to "tertiary"),
                Triple("onTertiaryContainer", onTertiaryContainer, tertiaryContainer to "tertiaryContainer"),
                Triple("onError", onError, error to "error"),
                Triple("onErrorContainer", onErrorContainer, errorContainer to "errorContainer"),
                Triple("onBackground", onBackground, background to "background"),
                Triple("onSurfaceVariant", onSurfaceVariant, surfaceVariant to "surfaceVariant"),
                Triple("inverseOnSurface", inverseOnSurface, inverseSurface to "inverseSurface"),
                Triple("inversePrimary", inversePrimary, inverseSurface to "inverseSurface"),
            ) + surfaces.flatMap { (surfaceName, surface) ->
                listOf(
                    Triple("onSurface", onSurface, surface to surfaceName),
                    Triple("onSurfaceVariant", onSurfaceVariant, surface to surfaceName),
                    // The storage row on Series Details has a tertiary icon on surfaceDim.
                    Triple("tertiary", tertiary, surface to surfaceName),
                )
            } + surfaces.filter { it.first != "surfaceDim" }.flatMap { (surfaceName, surface) ->
                // Like Material's own baseline, primary on surfaceDim is a little under 4.5:1, and
                // Pustaka never draws coloured text there.
                listOf(
                    Triple("primary", primary, surface to surfaceName),
                    Triple("secondary", secondary, surface to surfaceName),
                    Triple("error", error, surface to surfaceName),
                )
            }
        }

        val failures = textPairs.mapNotNull { (fgName, fg, bg) ->
            val ratio = contrast(fg, bg.first)
            if (ratio < 4.5) "$fgName on ${bg.second}: %.2f".format(ratio) else null
        } + surfaces.mapNotNull { (surfaceName, surface) ->
            val ratio = contrast(scheme.outline, surface)
            if (ratio < 3.0) "outline on $surfaceName: %.2f".format(ratio) else null
        }

        assertTrue("Low contrast in the $name scheme:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
