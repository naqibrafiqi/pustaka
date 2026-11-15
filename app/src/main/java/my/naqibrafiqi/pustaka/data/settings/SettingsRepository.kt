package my.naqibrafiqi.pustaka.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Qualifier

/** Light or dark for the app itself. The reader has its own theme, whose "Auto" follows this one. */
enum class AppTheme(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** How the Series tab is sorted (decision D18 keeps the choice). */
enum class SeriesSort(val label: String) {
    NAME("Name"),
    /** The series whose newest volume was added most recently first. */
    RECENTLY_ADDED("Recently added"),
    /** The series with the volume opened most recently first. Series never read come last. */
    RECENTLY_READ("Recently read"),
}

/** How the Series tab shows each series. */
enum class SeriesLayout(val label: String) {
    /** A card with the details and a strip of volumes. */
    CARDS("Cards"),
    /** Covers in a grid, with the name and number of volumes. */
    GRID("Compact grid"),
}

/** App settings. */
data class AppSettings(
    /** The folder the library is read from, `/storage/emulated/0/Pustaka` by default. */
    val libraryFolderPath: String,
    /** Whether the welcome screen has been shown, so a refusal leads to the "Grant access" screen. */
    val hasSeenWelcome: Boolean = false,
    /** False once another folder has been chosen in Settings. */
    val isDefaultLibraryFolder: Boolean = true,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    /** Uses colours taken from the phone's wallpaper, on Android 12 and newer. */
    val dynamicColor: Boolean = true,
    val seriesSort: SeriesSort = SeriesSort.NAME,
    val seriesLayout: SeriesLayout = SeriesLayout.CARDS,
    /** Library shows books in a grid, or in a list when false. */
    val libraryGrid: Boolean = true,
)

interface SettingsRepository {

    val settings: Flow<AppSettings>

    suspend fun setLibraryFolderPath(path: String)

    /** Goes back to the default library folder. */
    suspend fun resetLibraryFolderPath()

    suspend fun setHasSeenWelcome()

    suspend fun setAppTheme(theme: AppTheme)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setSeriesSort(sort: SeriesSort)

    suspend fun setSeriesLayout(layout: SeriesLayout)

    suspend fun setLibraryGrid(grid: Boolean)
}

/** The default library folder path. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultLibraryFolder

class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @param:DefaultLibraryFolder private val defaultLibraryFolderPath: String,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .map { preferences ->
            val chosenFolder = preferences[Keys.LIBRARY_FOLDER_PATH]
            AppSettings(
                libraryFolderPath = chosenFolder ?: defaultLibraryFolderPath,
                hasSeenWelcome = preferences[Keys.HAS_SEEN_WELCOME] ?: false,
                isDefaultLibraryFolder = chosenFolder == null || chosenFolder == defaultLibraryFolderPath,
                appTheme = preferences[Keys.APP_THEME].toEnum(AppTheme.SYSTEM),
                dynamicColor = preferences[Keys.DYNAMIC_COLOR] ?: true,
                seriesSort = preferences[Keys.SERIES_SORT].toEnum(SeriesSort.NAME),
                seriesLayout = preferences[Keys.SERIES_LAYOUT].toEnum(SeriesLayout.CARDS),
                libraryGrid = preferences[Keys.LIBRARY_GRID] ?: true,
            )
        }
        .distinctUntilChanged()

    override suspend fun setLibraryFolderPath(path: String) {
        val trimmed = path.trim().trimEnd('/')
        require(trimmed.isNotEmpty()) { "The library folder path can't be blank." }
        dataStore.edit { it[Keys.LIBRARY_FOLDER_PATH] = trimmed }
    }

    override suspend fun resetLibraryFolderPath() {
        dataStore.edit { it.remove(Keys.LIBRARY_FOLDER_PATH) }
    }

    override suspend fun setHasSeenWelcome() {
        dataStore.edit { it[Keys.HAS_SEEN_WELCOME] = true }
    }

    override suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { it[Keys.APP_THEME] = theme.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setSeriesSort(sort: SeriesSort) {
        dataStore.edit { it[Keys.SERIES_SORT] = sort.name }
    }

    override suspend fun setSeriesLayout(layout: SeriesLayout) {
        dataStore.edit { it[Keys.SERIES_LAYOUT] = layout.name }
    }

    override suspend fun setLibraryGrid(grid: Boolean) {
        dataStore.edit { it[Keys.LIBRARY_GRID] = grid }
    }

    /** A value saved by another version of the app that this one doesn't know falls back to [default]. */
    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        enumValues<T>().firstOrNull { it.name == this } ?: default

    private object Keys {
        val LIBRARY_FOLDER_PATH = stringPreferencesKey("library_folder_path")
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        val APP_THEME = stringPreferencesKey("app_theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SERIES_SORT = stringPreferencesKey("series_sort")
        val SERIES_LAYOUT = stringPreferencesKey("series_layout")
        val LIBRARY_GRID = booleanPreferencesKey("library_grid")
    }
}
