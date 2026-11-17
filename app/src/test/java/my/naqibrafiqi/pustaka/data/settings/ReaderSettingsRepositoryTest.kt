package my.naqibrafiqi.pustaka.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ReaderSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun startsWithTheDefaultsThenSavesAndResets() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempFolder.root, "settings.preferences_pb")
        }
        val repository = DataStoreReaderSettingsRepository(dataStore)
        assertEquals(ReaderSettings(), repository.settings.first())

        val chosen = ReaderSettings(
            theme = ReaderTheme.SEPIA,
            font = ReaderFont.SERIF,
            textSize = 1.2,
            lineSpacing = ReaderLineSpacing.NORMAL,
            margins = ReaderMargins.NARROW,
            scroll = true,
            justify = false,
        )
        repository.update(chosen)
        assertEquals(chosen, repository.settings.first())

        repository.reset()
        assertEquals(ReaderSettings(), repository.settings.first())
    }

    @Test
    fun valuesThisVersionDoesntKnowFallBackToTheDefaults() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempFolder.root, "settings.preferences_pb")
        }
        dataStore.edit { it[stringPreferencesKey("reader_theme")] = "NEON" }

        assertEquals(ReaderTheme.AUTO, DataStoreReaderSettingsRepository(dataStore).settings.first().theme)
    }
}
