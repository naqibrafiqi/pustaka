package my.naqibrafiqi.pustaka.ui.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.library.LibraryFiles
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultBookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultReadingRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultSeriesRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.data.settings.SeriesLayout
import my.naqibrafiqi.pustaka.data.settings.SeriesSort
import my.naqibrafiqi.pustaka.testing.FakeScans
import my.naqibrafiqi.pustaka.testing.FakeSettings
import my.naqibrafiqi.pustaka.testing.MainDispatcherRule
import my.naqibrafiqi.pustaka.testing.T0
import my.naqibrafiqi.pustaka.testing.TestClock
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryUiState
import my.naqibrafiqi.pustaka.ui.feature.library.screen.LibraryViewModel
import my.naqibrafiqi.pustaka.ui.feature.search.screen.SearchUiState
import my.naqibrafiqi.pustaka.ui.feature.search.screen.SearchViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.SeriesUiState
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.SeriesViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.sortSeries
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Series sorting and layout, the Library layout and search, with a real (in-memory) database. */
@RunWith(RobolectricTestRunner::class)
class SortAndLayoutTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private lateinit var db: PustakaDatabase
    private val clock = TestClock()
    private lateinit var series: SeriesRepository
    private lateinit var books: BookRepository
    private val settings = FakeSettings()

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        series = DefaultSeriesRepository(db, clock)
        books = DefaultBookRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    private fun seriesViewModel() =
        SeriesViewModel(series, books, settings, LibraryFiles(settings, series, books, FakeScans()))

    /** Adds a series with one volume, added [addedSeconds] after T0. */
    private suspend fun addSeries(name: String, addedSeconds: Long): Pair<Long, Long> {
        val seriesId = series.addSeries("/lib/$name", name)
        val bookId = books.addBook(
            book("$name, Vol. 01.epub", seriesId = seriesId, folder = "/lib/$name", addedAt = T0.plusSeconds(addedSeconds))
        )
        return seriesId to bookId
    }

    private suspend fun SeriesViewModel.names(sort: SeriesSort): List<String> =
        (state.first { it is SeriesUiState.Ready && it.sort == sort } as SeriesUiState.Ready).series.map { it.name }

    @Test
    fun seriesAreSortedAsChosenAndTheChoiceIsSaved() = runTest {
        addSeries("Alpha", addedSeconds = 30)
        val (_, betaVolume) = addSeries("Beta", addedSeconds = 10)
        addSeries("Gamma", addedSeconds = 20)
        clock.advanceSeconds(100)
        books.markOpened(betaVolume)
        val viewModel = seriesViewModel()

        assertEquals(listOf("Alpha", "Beta", "Gamma"), viewModel.names(SeriesSort.NAME))

        viewModel.selectSort(SeriesSort.RECENTLY_ADDED)
        assertEquals(listOf("Alpha", "Gamma", "Beta"), viewModel.names(SeriesSort.RECENTLY_ADDED))
        assertEquals(SeriesSort.RECENTLY_ADDED, settings.current.value.seriesSort)

        // Only Beta has been read. The others follow by name.
        viewModel.selectSort(SeriesSort.RECENTLY_READ)
        assertEquals(listOf("Beta", "Alpha", "Gamma"), viewModel.names(SeriesSort.RECENTLY_READ))
    }

    @Test
    fun anEmptySeriesCountsAsAddedWhenItsFolderWasFound() {
        val old = Series(id = 1, folderPath = "/a", folderName = "Old", createdAt = T0, updatedAt = T0)
        val empty = Series(id = 2, folderPath = "/b", folderName = "Empty", createdAt = T0.plusSeconds(50), updatedAt = T0)
        val volumes = mapOf<Long?, List<my.naqibrafiqi.pustaka.data.model.Book>>(
            1L to listOf(book("Old 1.epub", seriesId = 1, addedAt = T0.plusSeconds(10))),
        )

        assertEquals(listOf("Empty", "Old"), sortSeries(listOf(old, empty), volumes, SeriesSort.RECENTLY_ADDED).map { it.folderName })
    }

    @Test
    fun seriesLayoutTogglesAndIsSaved() = runTest {
        addSeries("Alpha", addedSeconds = 0)
        val viewModel = seriesViewModel()
        assertEquals(SeriesLayout.CARDS, (viewModel.state.first { it is SeriesUiState.Ready } as SeriesUiState.Ready).layout)

        viewModel.toggleLayout()

        viewModel.state.first { (it as? SeriesUiState.Ready)?.layout == SeriesLayout.GRID }
        assertEquals(SeriesLayout.GRID, settings.current.value.seriesLayout)
    }

    @Test
    fun libraryLayoutIsSaved() = runTest {
        books.addBook(book("Standalone.epub"))
        val viewModel = LibraryViewModel(books, DefaultReadingRepository(db, clock), settings, clock, SavedStateHandle())
        assertTrue((viewModel.state.first { it is LibraryUiState.Ready } as LibraryUiState.Ready).isGrid)

        viewModel.toggleLayout()

        viewModel.state.first { (it as? LibraryUiState.Ready)?.isGrid == false }
        assertFalse(settings.current.value.libraryGrid)

        // A new screen, for example after the app restarts, opens as a list.
        val reopened = LibraryViewModel(books, DefaultReadingRepository(db, clock), settings, clock, SavedStateHandle())
        assertFalse((reopened.state.first { it is LibraryUiState.Ready } as LibraryUiState.Ready).isGrid)
    }

    @Test
    fun searchSkipsHiddenItems() = runTest {
        val (seriesId, _) = addSeries("Brunhild", addedSeconds = 0)
        val hiddenBook = books.addBook(book("Brunhild Side Story.epub"))
        books.setHidden(hiddenBook, true)
        val viewModel = SearchViewModel(series, books, SavedStateHandle())
        assertEquals(SearchUiState.Idle, viewModel.state.first())

        viewModel.changeQuery("brunhild")

        val results = (viewModel.state.first { it is SearchUiState.Results } as SearchUiState.Results).results
        assertEquals(listOf("Brunhild"), results.series.map { it.title })
        assertEquals(listOf("Brunhild, Vol. 01"), results.books.map { it.title })

        series.setHidden(seriesId, true)
        val afterHiding = viewModel.state.first { (it as? SearchUiState.Results)?.results?.series?.isEmpty() == true }
        assertTrue((afterHiding as SearchUiState.Results).results.isEmpty)
    }
}
