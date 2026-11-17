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

/** The Readium theme for these settings. "Auto" follows the phone's dark mode. */
fun ReaderSettings.readiumTheme(systemDark: Boolean): Theme = when (theme) {
    ReaderTheme.AUTO -> if (systemDark) Theme.DARK else Theme.LIGHT
    ReaderTheme.LIGHT -> Theme.LIGHT
    ReaderTheme.SEPIA -> Theme.SEPIA
    ReaderTheme.DARK -> Theme.DARK
}

/** The settings in the form Readium's reader takes. */
fun ReaderSettings.toEpubPreferences(systemDark: Boolean): EpubPreferences = EpubPreferences(
    theme = readiumTheme(systemDark),
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
