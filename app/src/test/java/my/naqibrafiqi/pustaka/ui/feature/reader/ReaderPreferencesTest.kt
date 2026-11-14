package my.naqibrafiqi.pustaka.ui.feature.reader

import my.naqibrafiqi.pustaka.data.settings.ReaderFont
import my.naqibrafiqi.pustaka.data.settings.ReaderLineSpacing
import my.naqibrafiqi.pustaka.data.settings.ReaderMargins
import my.naqibrafiqi.pustaka.data.settings.ReaderSettings
import my.naqibrafiqi.pustaka.data.settings.ReaderTheme
import my.naqibrafiqi.pustaka.testing.book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.robolectric.RobolectricTestRunner

/** Readium's themes carry Android colours, so these run under Robolectric. */
@RunWith(RobolectricTestRunner::class)
class ReaderPreferencesTest {

    @Test
    fun autoFollowsThePhonesDarkMode() {
        val settings = ReaderSettings(theme = ReaderTheme.AUTO)
        assertEquals(Theme.LIGHT, settings.readiumTheme(systemDark = false))
        assertEquals(Theme.DARK, settings.readiumTheme(systemDark = true))
        // A chosen theme stays as it is.
        assertEquals(Theme.SEPIA, ReaderSettings(theme = ReaderTheme.SEPIA).readiumTheme(systemDark = true))
    }

    @Test
    fun theDefaultsKeepThePublishersLook() {
        val preferences = ReaderSettings().toEpubPreferences(systemDark = false)

        assertNull(preferences.fontFamily)
        assertNull(preferences.lineHeight)
        assertEquals(true, preferences.publisherStyles)
        assertEquals(1.0, preferences.fontSize!!, 0.0)
        assertEquals(false, preferences.scroll)
    }

    @Test
    fun chosenSettingsArePassedToReadium() {
        val preferences = ReaderSettings(
            theme = ReaderTheme.SEPIA,
            font = ReaderFont.SANS_SERIF,
            textSize = 1.3,
            lineSpacing = ReaderLineSpacing.LOOSE,
            margins = ReaderMargins.WIDE,
            scroll = true,
        ).toEpubPreferences(systemDark = false)

        assertEquals(Theme.SEPIA, preferences.theme)
        assertEquals(FontFamily.SANS_SERIF, preferences.fontFamily)
        assertEquals(1.3, preferences.fontSize!!, 0.0)
        assertEquals(1.8, preferences.lineHeight!!, 0.0)
        assertEquals(1.75, preferences.pageMargins!!, 0.0)
        // Line spacing only applies with the publisher's styles off.
        assertEquals(false, preferences.publisherStyles)
        assertEquals(true, preferences.scroll)
    }

    @Test
    fun textSizeStepsByTenPercentWithinItsRange() {
        var settings = ReaderSettings()
        repeat(3) { settings = settings.withTextSize(1) }
        assertEquals(1.3, settings.textSize, 0.0)
        assertEquals("130%", settings.textSizeLabel)

        repeat(100) { settings = settings.withTextSize(-1) }
        assertEquals(0.5, settings.textSize, 0.0)
        assertFalse(settings.canMakeTextSmaller)
        assertTrue(settings.canMakeTextBigger)

        repeat(100) { settings = settings.withTextSize(1) }
        assertEquals(2.5, settings.textSize, 0.0)
        assertFalse(settings.canMakeTextBigger)
    }

    @Test
    fun theNextVolumeSkipsMissingFiles() {
        val one = book("Vol. 01.epub", seriesId = 1, id = 1)
        val two = book("Vol. 02.epub", seriesId = 1, id = 2).copy(isMissing = true)
        val three = book("Vol. 03.epub", seriesId = 1, id = 3)
        val volumes = listOf(one, two, three)

        assertEquals(three, nextVolume(volumes, one))
        assertNull(nextVolume(volumes, three))
        // A book that isn't in the list, such as a standalone book.
        assertNull(nextVolume(volumes, book("Other.epub", id = 9)))
    }
}
