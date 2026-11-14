package my.naqibrafiqi.pustaka.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val default = "/storage/emulated/0/Pustaka"

    private fun TestScope.repository(file: File = File(tempFolder.root, "settings.preferences_pb")) =
        DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { file },
            default,
        )

    @Test
    fun usesTheDefaultFolderUntilOneIsChosen() = runTest {
        assertEquals(default, repository().settings.first().libraryFolderPath)
    }

    @Test
    fun savesAndResetsTheFolder() = runTest {
        val settings = repository()
        settings.setLibraryFolderPath(" /storage/emulated/0/Download/Pustaka/ ")
        assertEquals("/storage/emulated/0/Download/Pustaka", settings.settings.first().libraryFolderPath)

        settings.resetLibraryFolderPath()
        assertEquals(default, settings.settings.first().libraryFolderPath)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsABlankFolder() = runTest {
        repository().setLibraryFolderPath("  ")
    }
}
