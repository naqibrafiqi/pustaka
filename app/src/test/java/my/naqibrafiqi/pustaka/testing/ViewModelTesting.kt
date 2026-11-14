package my.naqibrafiqi.pustaka.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import my.naqibrafiqi.pustaka.data.settings.AppSettings
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Runs `viewModelScope` on a test dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Settings with a fixed library folder. */
class FakeSettings(path: String = "/storage/emulated/0/Pustaka") : SettingsRepository {
    override val settings: Flow<AppSettings> = MutableStateFlow(AppSettings(libraryFolderPath = path))
    override suspend fun setLibraryFolderPath(path: String) = error("not used")
    override suspend fun resetLibraryFolderPath() = error("not used")
    override suspend fun setHasSeenWelcome() = error("not used")
}
