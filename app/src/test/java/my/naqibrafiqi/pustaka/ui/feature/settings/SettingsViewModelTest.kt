package my.naqibrafiqi.pustaka.ui.feature.settings

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import my.naqibrafiqi.pustaka.data.library.access.PickedFolder
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultBookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultSeriesRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.data.settings.AppTheme
import my.naqibrafiqi.pustaka.data.storage.StorageReport
import my.naqibrafiqi.pustaka.data.storage.StorageUsage
import my.naqibrafiqi.pustaka.testing.FakeScans
import my.naqibrafiqi.pustaka.testing.FakeSettings
import my.naqibrafiqi.pustaka.testing.MainDispatcherRule
import my.naqibrafiqi.pustaka.testing.TestClock
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.FolderChange
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.SettingsEvent
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.SettingsUiState
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.SettingsViewModel
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.hidden.HiddenItemsUiState
import my.naqibrafiqi.pustaka.ui.feature.settings.screen.hidden.HiddenItemsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Settings and Hidden items, reading from a real (in-memory) database. */
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private lateinit var db: PustakaDatabase
    private val clock = TestClock()
    private lateinit var series: SeriesRepository
    private lateinit var books: BookRepository
    private val settings = FakeSettings()
    private val storage = FakeStorage()

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        series = DefaultSeriesRepository(db, clock)
        books = DefaultBookRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    private fun viewModel() =
        SettingsViewModel(settings, series, books, storage, FakeScans(), FakeSettings.DEFAULT_FOLDER)

    private suspend fun StateFlow<SettingsUiState?>.ready(): SettingsUiState = first { it != null }!!

    @Test
    fun countsHiddenSeriesAndBooks() = runTest {
        val viewModel = viewModel()
        assertEquals("Nothing hidden", viewModel.state.ready().hiddenLabel)

        val seriesId = series.addSeries("/lib/Brunhild", "Brunhild")
        series.setHidden(seriesId, true)
        listOf("A.epub", "B.epub").forEach { books.setHidden(books.addBook(book(it)), true) }

        assertEquals("1 series and 2 books", viewModel.state.first { it?.hiddenBooks == 2 }!!.hiddenLabel)
    }

    @Test
    fun asksBeforeSwitchingFoldersThenScans() = runTest {
        val viewModel = viewModel()
        val downloads = "/storage/emulated/0/Download/Pustaka"
        viewModel.onFolderPicked(PickedFolder.Path(downloads))

        assertEquals(FolderChange(downloads, isDefault = false), viewModel.folderChange.value)
        assertEquals(FakeSettings.DEFAULT_FOLDER, settings.current.value.libraryFolderPath)

        viewModel.confirmFolderChange()

        assertNull(viewModel.folderChange.value)
        assertEquals(downloads, settings.current.value.libraryFolderPath)
        assertEquals(SettingsEvent.Rescan, viewModel.events.first())
        assertFalse(viewModel.state.first { it?.libraryFolderPath == downloads }!!.isDefaultLibraryFolder)

        viewModel.useDefaultFolder()
        assertEquals(FolderChange(FakeSettings.DEFAULT_FOLDER, isDefault = true), viewModel.folderChange.value)
        viewModel.confirmFolderChange()
        assertEquals(FakeSettings.DEFAULT_FOLDER, settings.current.value.libraryFolderPath)
        assertEquals(SettingsEvent.Rescan, viewModel.events.first())
    }

    @Test
    fun cancellingKeepsTheFolder() = runTest {
        val viewModel = viewModel()
        viewModel.onFolderPicked(PickedFolder.Path("/storage/emulated/0/Books"))
        viewModel.dismissFolderChange()

        assertNull(viewModel.folderChange.value)
        assertEquals(FakeSettings.DEFAULT_FOLDER, settings.current.value.libraryFolderPath)
        assertNull(withTimeoutOrNull(100) { viewModel.events.firstOrNull() })
    }

    @Test
    fun explainsFoldersThatCantBeUsed() = runTest {
        val viewModel = viewModel()

        viewModel.onFolderPicked(PickedFolder.NotInternalStorage)
        val sdCard = viewModel.events.first() as SettingsEvent.Message
        assertTrue(sdCard.text.contains("internal storage"))

        viewModel.onFolderPicked(PickedFolder.Path(FakeSettings.DEFAULT_FOLDER))
        assertEquals(SettingsEvent.Message("That's already your library folder."), viewModel.events.first())
        assertNull(viewModel.folderChange.value)
    }

    @Test
    fun changesTheAppearance() = runTest {
        val viewModel = viewModel()
        viewModel.setAppTheme(AppTheme.DARK)
        viewModel.setDynamicColor(true)

        val state = viewModel.state.first { it?.appTheme == AppTheme.DARK && it.dynamicColor }
        assertEquals(AppTheme.DARK, state!!.appTheme)
    }

    @Test
    fun clearingCoversMeasuresAgainAndScans() = runTest {
        val viewModel = viewModel()
        assertEquals(1_000L, viewModel.storage.first { it != null }!!.covers)

        viewModel.clearCovers()

        assertEquals(SettingsEvent.Rescan, viewModel.events.first())
        assertEquals(1, storage.cleared)
        assertEquals(0L, viewModel.storage.first { it?.covers == 0L }!!.covers)
    }

    @Test
    fun hiddenItemsCanBeRestored() = runTest {
        val seriesId = series.addSeries("/lib/Brunhild", "Brunhild")
        series.setHidden(seriesId, true)
        val volume = books.addBook(book("Brunhild, Vol. 02.epub", seriesId = seriesId, folder = "/lib/Brunhild"))
        books.setHidden(volume, true)
        val standalone = books.addBook(book("Battle Divas.epub"))
        books.setHidden(standalone, true)
        val viewModel = HiddenItemsViewModel(series, books)

        val state = viewModel.state.first { it is HiddenItemsUiState.Ready } as HiddenItemsUiState.Ready
        assertEquals(listOf("Brunhild"), state.series.map { it.name })
        assertEquals(listOf("Folder: Brunhild"), state.series.map { it.description })
        assertEquals(
            mapOf("Battle Divas" to "Standalone book", "Brunhild, Vol. 02" to "In Brunhild"),
            state.books.associate { it.name to it.description },
        )

        viewModel.restore(state.series.single())

        assertEquals("“Brunhild” is back in your library.", viewModel.messages.first())
        val after = viewModel.state.first { (it as? HiddenItemsUiState.Ready)?.series?.isEmpty() == true }
        assertEquals(2, (after as HiddenItemsUiState.Ready).books.size)
        assertEquals(listOf("Brunhild"), series.observeSeries().first().map { it.name.value })
    }

    private class FakeStorage : StorageUsage {
        var covers = 1_000L
        var cleared = 0
        override suspend fun measure() = StorageReport(bookFiles = 10, bookCount = 1, covers = covers, appData = 5)
        override suspend fun clearCovers() {
            cleared++
            covers = 0
        }
    }
}
