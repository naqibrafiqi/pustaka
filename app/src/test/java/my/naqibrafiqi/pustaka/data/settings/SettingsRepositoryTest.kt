package my.naqibrafiqi.pustaka.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertTrue(settings.settings.first().isDefaultLibraryFolder)

        settings.setLibraryFolderPath(" /storage/emulated/0/Download/Pustaka/ ")
        assertEquals("/storage/emulated/0/Download/Pustaka", settings.settings.first().libraryFolderPath)
        assertFalse(settings.settings.first().isDefaultLibraryFolder)

        settings.resetLibraryFolderPath()
        assertEquals(default, settings.settings.first().libraryFolderPath)
        assertTrue(settings.settings.first().isDefaultLibraryFolder)
    }

    @Test
    fun choosingTheDefaultFolderCountsAsTheDefault() = runTest {
        val settings = repository()
        settings.setLibraryFolderPath("$default/")
        assertTrue(settings.settings.first().isDefaultLibraryFolder)
    }

    @Test
    fun savesTheSortAndLayout() = runTest {
        val settings = repository()
        val defaults = settings.settings.first()
        assertEquals(SeriesSort.NAME, defaults.seriesSort)
        assertEquals(SeriesLayout.CARDS, defaults.seriesLayout)
        assertTrue(defaults.libraryGrid)

        settings.setSeriesSort(SeriesSort.RECENTLY_READ)
        settings.setSeriesLayout(SeriesLayout.GRID)
        settings.setLibraryGrid(false)

        val saved = settings.settings.first()
        assertEquals(SeriesSort.RECENTLY_READ, saved.seriesSort)
        assertEquals(SeriesLayout.GRID, saved.seriesLayout)
        assertFalse(saved.libraryGrid)
    }

    @Test
    fun savesTheAppTheme() = runTest {
        val settings = repository()
        assertEquals(AppTheme.SYSTEM, settings.settings.first().appTheme)
        assertFalse(settings.settings.first().dynamicColor)

        settings.setAppTheme(AppTheme.DARK)
        settings.setDynamicColor(true)

        assertEquals(AppTheme.DARK, settings.settings.first().appTheme)
        assertTrue(settings.settings.first().dynamicColor)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsABlankFolder() = runTest {
        repository().setLibraryFolderPath("  ")
    }

    @Test
    fun remembersThatTheWelcomeScreenWasSeen() = runTest {
        val settings = repository()
        assertFalse(settings.settings.first().hasSeenWelcome)

        settings.setHasSeenWelcome()

        assertTrue(settings.settings.first().hasSeenWelcome)
    }
}
