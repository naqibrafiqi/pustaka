package my.naqibrafiqi.pustaka.data.settings

import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import my.naqibrafiqi.pustaka.R
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.math.roundToInt

enum class ReaderTheme(@param:StringRes val label: Int) {
    /** Light or dark, following the app's theme in Settings (which can follow the phone's). */
    AUTO(R.string.reader_theme_auto),
    LIGHT(R.string.reader_theme_light),
    SEPIA(R.string.reader_theme_sepia),
    DARK(R.string.reader_theme_dark),
}

enum class ReaderFont(@param:StringRes val label: Int) {
    /** The font chosen by the book's publisher. */
    ORIGINAL(R.string.reader_original),
    SERIF(R.string.reader_font_serif),
    SANS_SERIF(R.string.reader_font_sans_serif),
}

enum class ReaderLineSpacing(@param:StringRes val label: Int, val lineHeight: Double?) {
    /** The book's own spacing. */
    ORIGINAL(R.string.reader_original, null),
    TIGHT(R.string.reader_spacing_tight, 1.2),
    NORMAL(R.string.reader_normal, 1.5),
    LOOSE(R.string.reader_spacing_loose, 1.8),
}

enum class ReaderMargins(@param:StringRes val label: Int, val pageMargins: Double) {
    NARROW(R.string.reader_margins_narrow, 0.5),
    NORMAL(R.string.reader_normal, 1.0),
    WIDE(R.string.reader_margins_wide, 1.75),
}

/**
 * How books look in the reader. There's one set for every book (decision D8 in the roadmap), and
 * Phase 6's "Default reader settings" edits the same set.
 *
 * @property textSize a multiplier of the book's own text size, 1.0 being 100%.
 * @property scroll scrolls through each chapter instead of turning pages (decision D9).
 * @property justify lines up paragraphs with both edges of the page, unless the book aligns them
 *   itself, such as centred lines (decision D24).
 */
data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.AUTO,
    val font: ReaderFont = ReaderFont.ORIGINAL,
    val textSize: Double = 1.0,
    val lineSpacing: ReaderLineSpacing = ReaderLineSpacing.ORIGINAL,
    val margins: ReaderMargins = ReaderMargins.NORMAL,
    val scroll: Boolean = false,
    val justify: Boolean = true,
) {
    /** 100 for 100%. */
    val textSizePercent: Int get() = (textSize * 100).roundToInt()

    val canMakeTextSmaller: Boolean get() = textSize > TEXT_SIZE.start
    val canMakeTextBigger: Boolean get() = textSize < TEXT_SIZE.endInclusive

    /** Changes the text size by [steps] of 10%, staying within [TEXT_SIZE]. */
    fun withTextSize(steps: Int): ReaderSettings {
        // Rounded, so steps of 0.1 don't drift into 1.2000000000000002.
        val next = BigDecimal(textSize + TEXT_SIZE_STEP * steps).setScale(2, RoundingMode.HALF_UP).toDouble()
        return copy(textSize = next.coerceIn(TEXT_SIZE))
    }

    companion object {
        val TEXT_SIZE = 0.5..2.5
        const val TEXT_SIZE_STEP = 0.1
    }
}

interface ReaderSettingsRepository {

    val settings: Flow<ReaderSettings>

    suspend fun update(settings: ReaderSettings)

    suspend fun reset()
}

/** Kept in the same DataStore file as the other settings. */
class DataStoreReaderSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ReaderSettingsRepository {

    override val settings: Flow<ReaderSettings> = dataStore.data
        .map { preferences ->
            val defaults = ReaderSettings()
            ReaderSettings(
                theme = preferences[Keys.THEME].toEnum(defaults.theme),
                font = preferences[Keys.FONT].toEnum(defaults.font),
                textSize = (preferences[Keys.TEXT_SIZE] ?: defaults.textSize).coerceIn(ReaderSettings.TEXT_SIZE),
                lineSpacing = preferences[Keys.LINE_SPACING].toEnum(defaults.lineSpacing),
                margins = preferences[Keys.MARGINS].toEnum(defaults.margins),
                scroll = preferences[Keys.SCROLL] ?: defaults.scroll,
                justify = preferences[Keys.JUSTIFY] ?: defaults.justify,
            )
        }
        .distinctUntilChanged()

    override suspend fun update(settings: ReaderSettings) {
        dataStore.edit {
            it[Keys.THEME] = settings.theme.name
            it[Keys.FONT] = settings.font.name
            it[Keys.TEXT_SIZE] = settings.textSize
            it[Keys.LINE_SPACING] = settings.lineSpacing.name
            it[Keys.MARGINS] = settings.margins.name
            it[Keys.SCROLL] = settings.scroll
            it[Keys.JUSTIFY] = settings.justify
        }
    }

    override suspend fun reset() {
        dataStore.edit { preferences -> Keys.ALL.forEach { preferences.remove(it) } }
    }

    /** A value saved by another version of the app that this one doesn't know falls back to the default. */
    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        enumValues<T>().firstOrNull { it.name == this } ?: default

    private object Keys {
        val THEME = stringPreferencesKey("reader_theme")
        val FONT = stringPreferencesKey("reader_font")
        val TEXT_SIZE = doublePreferencesKey("reader_text_size")
        val LINE_SPACING = stringPreferencesKey("reader_line_spacing")
        val MARGINS = stringPreferencesKey("reader_margins")
        val SCROLL = booleanPreferencesKey("reader_scroll")
        val JUSTIFY = booleanPreferencesKey("reader_justify")
        val ALL = listOf(THEME, FONT, TEXT_SIZE, LINE_SPACING, MARGINS, SCROLL, JUSTIFY)
    }
}
