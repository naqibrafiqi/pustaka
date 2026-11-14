package my.naqibrafiqi.pustaka.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.math.roundToInt

enum class ReaderTheme(val label: String) {
    /** Light or dark, following the phone's dark mode. */
    AUTO("Auto"),
    LIGHT("Light"),
    SEPIA("Sepia"),
    DARK("Dark"),
}

enum class ReaderFont(val label: String) {
    /** The font chosen by the book's publisher. */
    ORIGINAL("Original"),
    SERIF("Serif"),
    SANS_SERIF("Sans serif"),
}

enum class ReaderLineSpacing(val label: String, val lineHeight: Double?) {
    /** The book's own spacing. */
    ORIGINAL("Original", null),
    TIGHT("Tight", 1.2),
    NORMAL("Normal", 1.5),
    LOOSE("Loose", 1.8),
}

enum class ReaderMargins(val label: String, val pageMargins: Double) {
    NARROW("Narrow", 0.5),
    NORMAL("Normal", 1.0),
    WIDE("Wide", 1.75),
}

/**
 * How books look in the reader. There's one set for every book (decision D8 in the roadmap), and
 * Phase 6's "Default reader settings" edits the same set.
 *
 * @property textSize a multiplier of the book's own text size, 1.0 being 100%.
 * @property scroll scrolls through each chapter instead of turning pages (decision D9).
 */
data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.AUTO,
    val font: ReaderFont = ReaderFont.ORIGINAL,
    val textSize: Double = 1.0,
    val lineSpacing: ReaderLineSpacing = ReaderLineSpacing.ORIGINAL,
    val margins: ReaderMargins = ReaderMargins.NORMAL,
    val scroll: Boolean = false,
) {
    /** "100%". */
    val textSizeLabel: String get() = "${(textSize * 100).roundToInt()}%"

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
        val ALL = listOf(THEME, FONT, TEXT_SIZE, LINE_SPACING, MARGINS, SCROLL)
    }
}
