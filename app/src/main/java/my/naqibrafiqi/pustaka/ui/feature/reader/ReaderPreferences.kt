package my.naqibrafiqi.pustaka.ui.feature.reader

import my.naqibrafiqi.pustaka.data.library.epub.PageStyles
import my.naqibrafiqi.pustaka.data.model.Book
import my.naqibrafiqi.pustaka.data.settings.ReaderFont
import my.naqibrafiqi.pustaka.data.settings.ReaderLineSpacing
import my.naqibrafiqi.pustaka.data.settings.ReaderSettings
import my.naqibrafiqi.pustaka.data.settings.ReaderTheme
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.preferences.Color as ReadiumColor

/**
 * The Readium theme for these settings. "Auto" follows the phone's dark mode. Readium has no dark
 * grey theme, so Dark grey is its Dark theme with Pustaka's own colours (see [pageColors]).
 */
fun ReaderSettings.readiumTheme(systemDark: Boolean): Theme = when (theme) {
    ReaderTheme.AUTO -> if (systemDark) Theme.DARK else Theme.LIGHT
    ReaderTheme.LIGHT -> Theme.LIGHT
    ReaderTheme.SEPIA -> Theme.SEPIA
    ReaderTheme.DARK, ReaderTheme.DARK_GREY -> Theme.DARK
}

/**
 * The colours of the page, as ARGB.
 *
 * @property isDark light text on a dark page, which also needs light system bar icons.
 */
data class PageColors(val background: Int, val text: Int, val isDark: Boolean)

/** The page's colours for these settings: Readium's theme's, or Pustaka's own for Dark grey (#55). */
fun ReaderSettings.pageColors(systemDark: Boolean): PageColors {
    if (theme == ReaderTheme.DARK_GREY) return PageColors(DARK_GREY_BACKGROUND, DARK_GREY_TEXT, isDark = true)
    val readium = readiumTheme(systemDark)
    return PageColors(readium.backgroundColor, readium.contentColor, isDark = readium == Theme.DARK)
}

/** Dark grey's page: light grey text on dark grey, softer than Dark's white on black (#55). */
const val DARK_GREY_BACKGROUND: Int = 0xFF303030.toInt()
const val DARK_GREY_TEXT: Int = 0xFFDCDCDC.toInt()

/** The settings in the form Readium's reader takes. */
fun ReaderSettings.toEpubPreferences(systemDark: Boolean): EpubPreferences = EpubPreferences(
    theme = readiumTheme(systemDark),
    // Only set for Dark grey. The other themes use Readium's own colours.
    backgroundColor = if (theme == ReaderTheme.DARK_GREY) ReadiumColor(DARK_GREY_BACKGROUND) else null,
    textColor = if (theme == ReaderTheme.DARK_GREY) ReadiumColor(DARK_GREY_TEXT) else null,
    fontFamily = when (font) {
        ReaderFont.ORIGINAL -> null
        ReaderFont.SERIF -> FontFamily.SERIF
        ReaderFont.SANS_SERIF -> FontFamily.SANS_SERIF
    },
    fontSize = textSize,
    lineHeight = lineSpacing.lineHeight,
    pageMargins = margins.pageMargins,
    // Readium only applies its own line spacing when the publisher's styles are turned off.
    publisherStyles = lineSpacing == ReaderLineSpacing.ORIGINAL,
    scroll = scroll,
    // Readium's own justifying needs the publisher's styles off and then pushes centred lines to
    // the left, so Pustaka's page script justifies instead. This only tells the script it's on.
    ligatures = if (justify) PageStyles.JUSTIFY_SIGNAL else null,
)

/**
 * The volume to offer after [current]: the next one in reading order whose file is there.
 * [volumes] must be in reading order. Null for the last volume or a standalone book.
 */
fun nextVolume(volumes: List<Book>, current: Book): Book? {
    val index = volumes.indexOfFirst { it.id == current.id }
    if (index < 0) return null
    return volumes.drop(index + 1).firstOrNull { !it.isMissing }
}
