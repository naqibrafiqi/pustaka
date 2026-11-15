package my.naqibrafiqi.pustaka.ui.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.library.CreateSeriesResult
import my.naqibrafiqi.pustaka.data.library.DeleteResult
import my.naqibrafiqi.pustaka.data.library.FolderContents
import my.naqibrafiqi.pustaka.data.library.LibraryFiles
import my.naqibrafiqi.pustaka.data.library.SeriesNames
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.Creator
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultBookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultReadingRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultSeriesRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.testing.FakeScans
import my.naqibrafiqi.pustaka.testing.FakeSettings
import my.naqibrafiqi.pustaka.testing.MainDispatcherRule
import my.naqibrafiqi.pustaka.testing.TestClock
import my.naqibrafiqi.pustaka.testing.book
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
import my.naqibrafiqi.pustaka.testing.text
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDeleteDialog
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDetailsViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.edit.EditBookViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.edit.EditDetailsViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.edit.EditSeriesViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.edit.EditUiState
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDeleteDialog
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDetailsViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.SeriesViewModel
import my.naqibrafiqi.pustaka.ui.model.DetailsEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Creating series, deleting series and books, and the Edit details forms. */
@RunWith(RobolectricTestRunner::class)
class EditAndDeleteTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var db: PustakaDatabase
    private val clock = TestClock()
    private lateinit var series: SeriesRepository
    private lateinit var books: BookRepository
    private lateinit var reading: ReadingRepository
    private lateinit var root: File
    private val scans = FakeScans()
    private lateinit var files: LibraryFiles

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        series = DefaultSeriesRepository(db, clock)
        books = DefaultBookRepository(db, clock)
        reading = DefaultReadingRepository(db, clock)
        root = temp.newFolder("Pustaka")
        files = LibraryFiles(FakeSettings(root.path), series, books, scans)
    }

    @After
    fun tearDown() = db.close()

    /** A series folder with [volumes] EPUB files (and their books) and [extras] other files. */
    private suspend fun seriesOnDisk(name: String, volumes: Int, extras: Int = 0): Pair<Long, List<Long>> {
        val folder = File(root, name).apply { mkdirs() }
        val id = series.addSeries(folder.path, name)
        val bookIds = (1..volumes).map { n ->
            File(folder, "$name $n.epub").writeText("epub $n")
            books.addBook(book("$name $n.epub", seriesId = id, folder = folder.path))
        }
        repeat(extras) { File(folder, "notes $it.txt").writeText("notes") }
        return id to bookIds
    }

    private suspend fun <T : EditDetailsViewModel> T.ready(): EditUiState.Ready =
        state.first { it is EditUiState.Ready } as EditUiState.Ready

    // --- Create series ---

    @Test
    fun creatingASeriesMakesAnEmptyFolderAndAddsIt() = runTest {
        val result = files.createSeries("  Harbour Tales ")

        val id = (result as CreateSeriesResult.Created).seriesId
        assertTrue(File(root, "Harbour Tales").isDirectory)
        assertEquals("Harbour Tales", series.getSeries(id)?.name?.value)
    }

    @Test
    fun aSeriesCantBeCreatedOverAnExistingFolder() = runTest {
        File(root, "Taken").mkdir()

        val result = files.createSeries("Taken") as CreateSeriesResult.Invalid
        assertEquals("There's already a folder called \"Taken\".", result.message.text())
    }

    @Test
    fun seriesNamesFollowTheFolderRules() {
        assertNull(SeriesNames.problemWith("Harbour Tales, Vol. 1-4"))
        assertEquals("Enter a name.", SeriesNames.problemWith("   ")?.text())
        assertNotNull(SeriesNames.problemWith(".hidden"))
        assertEquals("The name can't contain / :", SeriesNames.problemWith("a/b:c")?.text())
        assertNotNull(SeriesNames.problemWith("x".repeat(256)))
    }

    @Test
    fun theSeriesTabOpensTheNewSeries() = runTest {
        val viewModel = SeriesViewModel(series, books, FakeSettings(root.path), files)
        viewModel.openCreateDialog()
        viewModel.changeNewName("New One")

        viewModel.createSeries()

        val id = viewModel.created.first()
        assertEquals("New One", series.getSeries(id)?.folderName)
        assertNull(viewModel.createDialog.value)
    }

    @Test
    fun theSeriesTabShowsWhyANameCantBeUsed() = runTest {
        val viewModel = SeriesViewModel(series, books, FakeSettings(root.path), files)
        viewModel.openCreateDialog()
        viewModel.changeNewName(".dot")

        viewModel.createSeries()

        val dialog = viewModel.createDialog.first { it?.error != null }!!
        assertFalse(dialog.isCreating)
        assertFalse(File(root, ".dot").exists())
    }

    // --- Delete series ---

    @Test
    fun folderContentsCountEveryFileAndTheEpubs() = runTest {
        seriesOnDisk("Harbour", volumes = 3, extras = 2)

        assertEquals(FolderContents(files = 5, epubs = 3), files.contentsOf(File(root, "Harbour").path))
    }

    @Test
    fun deletingASeriesFolderDeletesTheFilesAndTheSeries() = runTest {
        val (id, bookIds) = seriesOnDisk("Harbour", volumes = 2, extras = 1)

        assertEquals(DeleteResult.Deleted, files.deleteSeriesFolder(id))

        assertFalse(File(root, "Harbour").exists())
        assertNull(series.getSeries(id))
        assertNull(books.getBook(bookIds[0]))
    }

    @Test
    fun seriesDetailsAsksBeforeDeletingFiles() = runTest {
        val (id, _) = seriesOnDisk("Harbour", volumes = 2)
        val viewModel = SeriesDetailsViewModel(id, series, books, reading, files, SavedStateHandle())

        viewModel.requestDelete()
        val choose = viewModel.deleteDialog.first { it != null } as SeriesDeleteDialog.Choose
        assertEquals(FolderContents(files = 2, epubs = 2), choose.contents)

        viewModel.chooseDeleteFiles()
        assertTrue(viewModel.deleteDialog.value is SeriesDeleteDialog.ConfirmDeleteFiles)

        viewModel.deleteFolder()
        assertEquals(DetailsEvent.Closed, viewModel.events.first())
        assertFalse(File(root, "Harbour").exists())
    }

    @Test
    fun anEmptySeriesOnlyNeedsOneConfirmation() = runTest {
        val (id, _) = seriesOnDisk("Empty", volumes = 0)
        val viewModel = SeriesDetailsViewModel(id, series, books, reading, files, SavedStateHandle())

        viewModel.requestDelete()

        assertTrue(viewModel.deleteDialog.first { it != null } is SeriesDeleteDialog.ConfirmDeleteEmptyFolder)
    }

    @Test
    fun removingASeriesFromTheLibraryHidesItAndKeepsTheFolder() = runTest {
        val (id, _) = seriesOnDisk("Harbour", volumes = 1)
        val viewModel = SeriesDetailsViewModel(id, series, books, reading, files, SavedStateHandle())

        viewModel.removeFromLibrary()

        assertEquals(DetailsEvent.Closed, viewModel.events.first())
        assertTrue(series.getSeries(id)!!.isHidden)
        assertTrue(File(root, "Harbour").isDirectory)
    }

    // --- Delete book ---

    @Test
    fun deletingABookFileDeletesTheBookAndRescansTheSeries() = runTest {
        val (_, bookIds) = seriesOnDisk("Harbour", volumes = 2)
        val viewModel = BookDetailsViewModel(bookIds[0], books, reading, series, files)

        viewModel.requestDelete()
        assertTrue(viewModel.deleteDialog.first { it != null } is BookDeleteDialog.Choose)
        viewModel.chooseDeleteFile()
        viewModel.deleteFile()

        assertEquals(DetailsEvent.Closed, viewModel.events.first())
        assertFalse(File(root, "Harbour/Harbour 1.epub").exists())
        assertTrue(File(root, "Harbour/Harbour 2.epub").exists())
        assertNull(books.getBook(bookIds[0]))
        assertEquals(1, scans.requested)
    }

    @Test
    fun aMissingBookCanOnlyBeRemovedForGood() = runTest {
        val id = books.addBook(book("gone.epub", folder = root.path))
        db.bookDao().setMissing(id, true)
        val viewModel = BookDetailsViewModel(id, books, reading, series, files)

        viewModel.requestDelete()
        assertTrue(viewModel.deleteDialog.first { it != null } is BookDeleteDialog.ConfirmForget)
        viewModel.forget()

        assertEquals(DetailsEvent.Closed, viewModel.events.first())
        assertNull(books.getBook(id))
    }

    // --- Edit series ---

    @Test
    fun editingASeriesOnlyKeepsWhatChanged() = runTest {
        val id = series.addSeries("/lib/hb", "[Group] Harbour", SeriesDetails(author = "Ren Okada", tags = listOf("Comedy")))
        val viewModel = EditSeriesViewModel(id, series, books)
        assertEquals("[Group] Harbour", viewModel.ready().value(EditSeriesViewModel.NAME))

        viewModel.change(EditSeriesViewModel.NAME, "Harbour Tales")
        viewModel.change(EditSeriesViewModel.TAGS, "Comedy, Mystery")
        viewModel.save()
        viewModel.saved.first()

        val saved = series.getSeries(id)!!
        assertEquals("Harbour Tales", saved.editedName)
        assertEquals(SeriesDetails(tags = listOf("Comedy", "Mystery")), saved.editedDetails)
    }

    @Test
    fun useDetailsFromFileTakesTheSeriesTitleFromAVolume() = runTest {
        val id = series.addSeries("/lib/hb", "[Group] Harbour v01-04")
        books.addBook(
            book("v01.epub", seriesId = id, folder = "/lib/hb")
                .copy(sourceDetails = BookDetails(seriesTitle = "Harbour Tales"))
        )
        val viewModel = EditSeriesViewModel(id, series, books)
        assertTrue(viewModel.ready().canUseFileDetails)

        viewModel.useDetailsFromFile()

        val form = viewModel.ready()
        assertEquals("Harbour Tales", form.value(EditSeriesViewModel.NAME))
        assertTrue(form.hasChanges)
    }

    @Test
    fun resettingAFieldGoesBackToItsDefault() = runTest {
        val id = series.addSeries("/lib/hb", "Harbour", SeriesDetails(author = "Ren Okada"))
        series.updateEdits(id, name = null, details = SeriesDetails(author = "Someone Else"))
        val viewModel = EditSeriesViewModel(id, series, books)
        assertTrue(viewModel.ready().fields.first { it.key == EditSeriesViewModel.AUTHOR }.isEdited)

        viewModel.reset(EditSeriesViewModel.AUTHOR)
        viewModel.save()
        viewModel.saved.first()

        assertNull(series.getSeries(id)!!.editedDetails.author)
    }

    // --- Edit book ---

    @Test
    fun useDetailsFromFileNamesTheBookAfterItsTitle() = runTest {
        val id = books.addBook(
            book("[Group] Harbour v04 (Digital).epub").copy(sourceDetails = BookDetails(title = "Harbour Tales 4"))
        )
        val viewModel = EditBookViewModel(id, books)
        viewModel.ready()

        viewModel.useDetailsFromFile()
        viewModel.save()
        viewModel.saved.first()

        assertEquals("Harbour Tales 4", books.getBook(id)!!.name.value)
    }

    @Test
    fun aVolumeNumberMustBeANumber() = runTest {
        val seriesId = series.addSeries("/lib/hb", "Harbour")
        val id = books.addBook(book("Harbour 4.epub", seriesId = seriesId, folder = "/lib/hb"))
        val viewModel = EditBookViewModel(id, books)
        assertEquals("4", viewModel.ready().value(EditBookViewModel.VOLUME))

        viewModel.change(EditBookViewModel.VOLUME, "four")
        viewModel.save()

        val volume = viewModel.ready().fields.first { it.key == EditBookViewModel.VOLUME }
        assertNotNull(volume.error)
        assertNull(books.getBook(id)!!.editedDetails.volumeNumber)

        viewModel.change(EditBookViewModel.VOLUME, "4.5")
        viewModel.save()
        viewModel.saved.first()
        assertEquals(4.5, books.getBook(id)!!.volumeNumber!!, 0.0)
    }

    @Test
    fun savingWithoutChangesStoresNoEdits() = runTest {
        val id = books.addBook(
            book("a.epub").copy(
                sourceDetails = BookDetails(
                    creators = listOf(Creator("Ren Okada"), Creator("Lio Park", Creator.ROLE_ILLUSTRATOR)),
                    subjects = listOf("Mystery"),
                ),
            )
        )
        val viewModel = EditBookViewModel(id, books)
        viewModel.ready()

        viewModel.save()
        viewModel.saved.first()

        assertEquals(BookDetails.EMPTY, books.getBook(id)!!.editedDetails)
        assertNull(books.getBook(id)!!.editedName)
    }

    @Test
    fun editingAuthorsKeepsOtherCredits() = runTest {
        val id = books.addBook(
            book("a.epub").copy(
                sourceDetails = BookDetails(
                    creators = listOf(Creator("Ren Okada"), Creator("A Translator", Creator.ROLE_TRANSLATOR)),
                ),
            )
        )
        val viewModel = EditBookViewModel(id, books)
        viewModel.ready()

        viewModel.change(EditBookViewModel.AUTHORS, "Ren Okada, Co Writer")
        viewModel.save()
        viewModel.saved.first()

        assertEquals(
            listOf(
                Creator("Ren Okada", Creator.ROLE_AUTHOR),
                Creator("Co Writer", Creator.ROLE_AUTHOR),
                Creator("A Translator", Creator.ROLE_TRANSLATOR),
            ),
            books.getBook(id)!!.editedDetails.creators,
        )
    }
}
