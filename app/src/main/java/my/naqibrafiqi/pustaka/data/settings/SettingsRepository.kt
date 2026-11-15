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
)

interface SettingsRepository {

    val settings: Flow<AppSettings>

    suspend fun setLibraryFolderPath(path: String)

    /** Goes back to the default library folder. */
    suspend fun resetLibraryFolderPath()

    suspend fun setHasSeenWelcome()

    suspend fun setAppTheme(theme: AppTheme)

    suspend fun setDynamicColor(enabled: Boolean)
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
                appTheme = AppTheme.entries.firstOrNull { it.name == preferences[Keys.APP_THEME] } ?: AppTheme.SYSTEM,
                dynamicColor = preferences[Keys.DYNAMIC_COLOR] ?: true,
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

    private object Keys {
        val LIBRARY_FOLDER_PATH = stringPreferencesKey("library_folder_path")
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        val APP_THEME = stringPreferencesKey("app_theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
