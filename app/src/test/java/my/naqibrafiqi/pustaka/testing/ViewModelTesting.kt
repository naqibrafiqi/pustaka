package my.naqibrafiqi.pustaka.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import my.naqibrafiqi.pustaka.data.library.scan.LibraryScans
import my.naqibrafiqi.pustaka.data.library.scan.ScanStatus
import my.naqibrafiqi.pustaka.data.settings.AppSettings
import my.naqibrafiqi.pustaka.data.settings.AppTheme
import my.naqibrafiqi.pustaka.data.settings.SeriesLayout
import my.naqibrafiqi.pustaka.data.settings.SeriesSort
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Runs `viewModelScope` on a test dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Settings kept in memory, starting with the library folder at [path]. */
class FakeSettings(
    path: String = DEFAULT_FOLDER,
    private val defaultPath: String = DEFAULT_FOLDER,
) : SettingsRepository {
    val current = MutableStateFlow(AppSettings(libraryFolderPath = path, isDefaultLibraryFolder = path == defaultPath))
    override val settings: Flow<AppSettings> = current

    override suspend fun setLibraryFolderPath(path: String) =
        current.update { it.copy(libraryFolderPath = path, isDefaultLibraryFolder = path == defaultPath) }

    override suspend fun resetLibraryFolderPath() = setLibraryFolderPath(defaultPath)
    override suspend fun setHasSeenWelcome() = current.update { it.copy(hasSeenWelcome = true) }
    override suspend fun setAppTheme(theme: AppTheme) = current.update { it.copy(appTheme = theme) }
    override suspend fun setDynamicColor(enabled: Boolean) = current.update { it.copy(dynamicColor = enabled) }
    override suspend fun setSeriesSort(sort: SeriesSort) = current.update { it.copy(seriesSort = sort) }
    override suspend fun setSeriesLayout(layout: SeriesLayout) = current.update { it.copy(seriesLayout = layout) }
    override suspend fun setLibraryGrid(grid: Boolean) = current.update { it.copy(libraryGrid = grid) }

    companion object {
        const val DEFAULT_FOLDER = "/storage/emulated/0/Pustaka"
    }
}

/** Counts the scans asked for, without running any. */
class FakeScans : LibraryScans {
    var requested = 0
    override val status: Flow<ScanStatus> = MutableStateFlow(ScanStatus.IDLE)
    override fun requestScan() {
        requested++
    }
}
