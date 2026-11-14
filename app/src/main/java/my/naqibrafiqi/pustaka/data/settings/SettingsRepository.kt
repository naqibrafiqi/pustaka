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

/** App settings. More are added in Phase 6. */
data class AppSettings(
    /** The folder the library is read from, `/storage/emulated/0/Pustaka` by default. */
    val libraryFolderPath: String,
    /** Whether the welcome screen has been shown, so a refusal leads to the "Grant access" screen. */
    val hasSeenWelcome: Boolean = false,
)

interface SettingsRepository {

    val settings: Flow<AppSettings>

    suspend fun setLibraryFolderPath(path: String)

    /** Goes back to the default library folder. */
    suspend fun resetLibraryFolderPath()

    suspend fun setHasSeenWelcome()
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
            AppSettings(
                libraryFolderPath = preferences[Keys.LIBRARY_FOLDER_PATH] ?: defaultLibraryFolderPath,
                hasSeenWelcome = preferences[Keys.HAS_SEEN_WELCOME] ?: false,
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

    private object Keys {
        val LIBRARY_FOLDER_PATH = stringPreferencesKey("library_folder_path")
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
    }
}
