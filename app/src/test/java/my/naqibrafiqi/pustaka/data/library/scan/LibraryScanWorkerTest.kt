package my.naqibrafiqi.pustaka.data.library.scan

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.library.access.StorageAccess
import my.naqibrafiqi.pustaka.data.library.epub.CoverStore
import my.naqibrafiqi.pustaka.data.library.epub.EpubMetadata
import my.naqibrafiqi.pustaka.data.library.epub.EpubReader
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.settings.AppSettings
import my.naqibrafiqi.pustaka.data.settings.AppTheme
import my.naqibrafiqi.pustaka.data.settings.SeriesLayout
import my.naqibrafiqi.pustaka.data.settings.SeriesSort
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import my.naqibrafiqi.pustaka.testing.T0
import my.naqibrafiqi.pustaka.testing.TestClock
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LibraryScanWorkerTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var db: PustakaDatabase
    private lateinit var library: File
    private val clock = TestClock()
    private var hasAccess = true

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        library = File(temp.root, "storage/Pustaka")
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun createsTheFolderAndScans() = runTest {
        val result = runWorker()

        assertTrue(library.isDirectory)
        assertEquals(ListenableWorker.Result.success(ScanWorkData.encode(ScanOutcome.Finished(ScanResult.Success()), T0)), result)
    }

    @Test
    fun reportsWhatTheScanFound() = runTest {
        library.mkdirs()
        File(library, "Book.epub").writeText("book")
        File(library, "Series").mkdirs()

        val output = (runWorker() as ListenableWorker.Result.Success).outputData

        assertEquals(
            T0 to ScanOutcome.Finished(ScanResult.Success(newBooks = 1, newSeries = 1)),
            ScanWorkData.decode(output),
        )
    }

    @Test
    fun withoutAccessNothingIsScannedOrCreated() = runTest {
        hasAccess = false

        val result = runWorker()

        assertEquals(ListenableWorker.Result.failure(ScanWorkData.encode(ScanOutcome.NoAccess, T0)), result)
        assertTrue(!library.exists())
    }

    private suspend fun runWorker(): ListenableWorker.Result {
        val settings = object : SettingsRepository {
            override val settings: Flow<AppSettings> = MutableStateFlow(AppSettings(library.path))
            override suspend fun setLibraryFolderPath(path: String) = error("not used")
            override suspend fun resetLibraryFolderPath() = error("not used")
            override suspend fun setHasSeenWelcome() = error("not used")
            override suspend fun setAppTheme(theme: AppTheme) = error("not used")
            override suspend fun setDynamicColor(enabled: Boolean) = error("not used")
            override suspend fun setSeriesSort(sort: SeriesSort) = error("not used")
            override suspend fun setSeriesLayout(layout: SeriesLayout) = error("not used")
            override suspend fun setLibraryGrid(grid: Boolean) = error("not used")
        }
        val reader = object : EpubReader {
            override suspend fun read(file: File, fingerprint: String) = EpubMetadata(BookDetails(title = "Title"), 10)
        }
        val scanner = LibraryScanner(db, settings, reader, CoverStore(temp.newFolder()), clock)
        val access = object : StorageAccess {
            override fun hasAccess() = hasAccess
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<LibraryScanWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    LibraryScanWorker(appContext, workerParameters, scanner, access, settings, clock)
            })
            .build()
            .doWork()
    }
}
