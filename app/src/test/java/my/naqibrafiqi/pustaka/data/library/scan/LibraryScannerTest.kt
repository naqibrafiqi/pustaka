package my.naqibrafiqi.pustaka.data.library.scan

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.library.epub.CoverStore
import my.naqibrafiqi.pustaka.data.library.epub.EpubMetadata
import my.naqibrafiqi.pustaka.data.library.epub.EpubReader
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.model.BookDetails
import my.naqibrafiqi.pustaka.data.model.Creator
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import my.naqibrafiqi.pustaka.data.repository.DefaultBookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultReadingRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultSeriesRepository
import my.naqibrafiqi.pustaka.data.settings.AppSettings
import my.naqibrafiqi.pustaka.data.settings.AppTheme
import my.naqibrafiqi.pustaka.data.settings.SeriesLayout
import my.naqibrafiqi.pustaka.data.settings.SeriesSort
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import my.naqibrafiqi.pustaka.testing.TestClock
import my.naqibrafiqi.pustaka.testing.inMemoryDatabase
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
import java.util.UUID

/**
 * Runs the scanner against a real folder. The EPUB reader is a fake: each test "EPUB" is a text file
 * holding the title and volume number the fake reports, plus a random part so every file has its
 * own fingerprint.
 */
@RunWith(RobolectricTestRunner::class)
class LibraryScannerTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var db: PustakaDatabase
    private lateinit var library: File
    private lateinit var coverDir: File
    private val clock = TestClock()
    private val reader = FakeEpubReader()
    private lateinit var scanner: LibraryScanner

    private val books by lazy { DefaultBookRepository(db, clock) }
    private val series by lazy { DefaultSeriesRepository(db, clock) }
    private val reading by lazy { DefaultReadingRepository(db, clock) }

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        library = temp.newFolder("Pustaka")
        coverDir = temp.newFolder("covers")
        scanner = LibraryScanner(db, FakeSettings(library.path), reader, CoverStore(coverDir), clock)
    }

    @After
    fun tearDown() = db.close()

    // --- Folder layout ---

    @Test
    fun followsTheFolderLayout() = runTest {
        epub("The Left Hand of Darkness.epub", title = "The Left Hand of Darkness")
        epub("Playing Death Games/Vol. 01.epub", title = "PDG 1", volume = 1.0)
        epub("Playing Death Games/Vol. 02.epub", title = "PDG 2", volume = 2.0)
        epub("Playing Death Games/extras/Deeper.epub")
        File(library, "My New Series").mkdirs()
        epub(".hidden/Secret.epub")
        epub(".Dotfile.epub")
        File(library, "notes.txt").writeText("not a book")
        File(library, "Playing Death Games/cover.jpg").writeText("not a book")
        epub("UPPERCASE.EPUB")

        val result = scanner.scan() as ScanResult.Success

        assertEquals(4, result.newBooks)
        assertEquals(2, result.newSeries)
        assertEquals(listOf("My New Series", "Playing Death Games"), series.observeSeries().first().map { it.name.value })
        assertEquals(listOf("The Left Hand of Darkness", "UPPERCASE"), books.observeStandaloneBooks().first().map { it.name.value })
        val pdg = series.getSeriesByFolderPath(File(library, "Playing Death Games").path)!!
        assertEquals(listOf("Vol. 01", "Vol. 02"), books.observeVolumes(pdg.id).first().map { it.name.value })
        assertEquals("PDG 1", books.observeVolumes(pdg.id).first().first().details.title)
    }

    @Test
    fun aScanWithNothingNewReadsNoFiles() = runTest {
        epub("A.epub")
        epub("Series/B.epub")
        scanner.scan()
        reader.reads.clear()

        val result = scanner.scan() as ScanResult.Success

        assertFalse(result.hasChanges)
        assertEquals(emptyList<String>(), reader.reads)
    }

    @Test
    fun aMissingLibraryFolderChangesNothing() = runTest {
        epub("A.epub")
        scanner.scan()
        library.deleteRecursively()

        val result = scanner.scan()

        assertEquals(ScanResult.FolderUnavailable(library.path), result)
        assertFalse(db.bookDao().getAll().single().isMissing)
    }

    // --- Identity ---

    @Test
    fun aRenamedFileKeepsItsBookAndProgress() = runTest {
        val file = epub("Series/[Group] Series v01 (Digital).epub", title = "Vol 1")
        scanner.scan()
        val before = db.bookDao().getAll().single()
        reading.saveProgress(before.id, locator = "{}", progression = 0.4, position = 12)
        books.updateEdits(before.id, name = null, details = BookDetails(description = "My note"))
        reader.reads.clear()

        file.renameTo(File(file.parentFile, "Series 01.epub"))
        val result = scanner.scan() as ScanResult.Success

        assertEquals(1, result.movedBooks)
        assertEquals(0, result.newBooks)
        assertEquals(0, result.missingBooks)
        assertEquals(emptyList<String>(), reader.reads)
        val after = books.getBook(before.id)!!
        assertEquals("Series 01.epub", after.fileName)
        assertEquals("Series 01", after.name.value)
        assertEquals("My note", after.details.description)
        assertEquals(0.4, reading.observeProgress(before.id).first()!!.progression, 0.0)
    }

    @Test
    fun aFileMovedToAnotherSeriesChangesSeries() = runTest {
        val file = epub("First/Book.epub")
        File(library, "Second").mkdirs()
        scanner.scan()
        val id = db.bookDao().getAll().single().id

        file.renameTo(File(library, "Second/Book.epub"))
        scanner.scan()

        val second = series.getSeriesByFolderPath(File(library, "Second").path)!!
        assertEquals(second.id, books.getBook(id)!!.seriesId)
    }

    @Test
    fun aFileMovedOutOfASeriesBecomesStandalone() = runTest {
        val file = epub("Series/Book.epub")
        scanner.scan()
        val id = db.bookDao().getAll().single().id

        file.renameTo(File(library, "Book.epub"))
        scanner.scan()

        assertTrue(books.getBook(id)!!.isStandalone)
    }

    @Test
    fun aRenamedSeriesFolderKeepsTheSeriesAndItsEdits() = runTest {
        epub("Death Games/Vol 1.epub")
        epub("Death Games/Vol 2.epub")
        scanner.scan()
        val before = series.getSeriesByFolderPath(File(library, "Death Games").path)!!
        series.updateEdits(before.id, name = "My Name", details = SeriesDetails(synopsis = "Mine"))

        File(library, "Death Games").renameTo(File(library, "Playing Death Games"))
        val result = scanner.scan() as ScanResult.Success

        assertEquals(0, result.newSeries)
        assertEquals(0, result.removedSeries)
        assertEquals(2, result.movedBooks)
        val after = series.getSeries(before.id)!!
        assertEquals(File(library, "Playing Death Games").path, after.folderPath)
        assertEquals("Playing Death Games", after.folderName)
        assertEquals("My Name", after.name.value)
        assertEquals("Mine", after.details.synopsis)
        assertEquals(1, series.observeSeries().first().size)
    }

    @Test
    fun anIdenticalCopyIsASecondBook() = runTest {
        val original = epub("A.epub")
        original.copyTo(File(library, "Copy of A.epub"))

        val result = scanner.scan() as ScanResult.Success

        assertEquals(2, result.newBooks)
    }

    // --- Missing files ---

    @Test
    fun aDeletedFileIsMarkedMissingAndRestoredWhenItComesBack() = runTest {
        val file = epub("Series/Book.epub")
        scanner.scan()
        val id = db.bookDao().getAll().single().id
        reading.saveProgress(id, locator = "{}", progression = 0.5, position = 3)
        val backup = File(temp.root, "backup.epub")
        file.copyTo(backup)
        backup.setLastModified(file.lastModified())
        file.delete()

        val missing = scanner.scan() as ScanResult.Success
        assertEquals(1, missing.missingBooks)
        assertTrue(books.getBook(id)!!.isMissing)
        assertEquals(1, books.observeLibraryBooks().first().size) // still shown, greyed out

        val again = scanner.scan() as ScanResult.Success
        assertFalse("missing books are only counted once", again.hasChanges)

        backup.copyTo(File(library, "Elsewhere.epub"))
        val restored = scanner.scan() as ScanResult.Success
        assertEquals(1, restored.restoredBooks)
        val book = books.getBook(id)!!
        assertFalse(book.isMissing)
        assertTrue(book.isStandalone)
        assertEquals(0.5, reading.observeProgress(id).first()!!.progression, 0.0)
    }

    @Test
    fun aDeletedSeriesFolderWithNoBooksLeftIsRemoved() = runTest {
        File(library, "Empty").mkdirs()
        scanner.scan()

        File(library, "Empty").delete()
        val result = scanner.scan() as ScanResult.Success

        assertEquals(1, result.removedSeries)
        assertEquals(emptyList<Any>(), series.observeSeries().first())
    }

    @Test
    fun aDeletedSeriesFolderIsKeptWhileItHasMissingBooks() = runTest {
        epub("Series/Book.epub")
        scanner.scan()

        File(library, "Series").deleteRecursively()
        val result = scanner.scan() as ScanResult.Success

        assertEquals(1, result.missingBooks)
        assertEquals(0, result.removedSeries)
        assertEquals(1, series.observeSeries().first().size)
    }

    // --- Changed files ---

    @Test
    fun aChangedFileIsReadAgainAndKeepsItsEdits() = runTest {
        val file = epub("Book.epub", title = "Old title")
        scanner.scan()
        val id = db.bookDao().getAll().single().id
        books.updateEdits(id, name = "My name", details = BookDetails.EMPTY)

        writeFake(file, title = "New title")
        file.setLastModified(file.lastModified() + 10_000)
        val result = scanner.scan() as ScanResult.Success

        assertEquals(1, result.updatedBooks)
        val book = books.getBook(id)!!
        assertEquals("New title", book.details.title)
        assertEquals("My name", book.name.value)
    }

    @Test
    fun anUnreadableFileIsStillAddedWithItsFilename() = runTest {
        epub("Broken.epub", title = UNREADABLE)

        val result = scanner.scan() as ScanResult.Success

        assertEquals(1, result.newBooks)
        assertEquals(listOf(File(library, "Broken.epub").path), result.unreadableFiles)
        val book = books.observeStandaloneBooks().first().single()
        assertEquals("Broken", book.name.value)
        assertEquals(BookDetails.EMPTY, book.sourceDetails)
    }

    // --- Hidden items ---

    @Test
    fun hiddenSeriesAndBooksStayHidden() = runTest {
        epub("Hidden Series/Vol 1.epub")
        epub("Hidden Book.epub")
        scanner.scan()
        val hiddenSeries = series.getSeriesByFolderPath(File(library, "Hidden Series").path)!!
        series.setHidden(hiddenSeries.id, true)
        books.setHidden(books.observeStandaloneBooks().first().single().id, true)

        val result = scanner.scan() as ScanResult.Success

        assertFalse(result.hasChanges)
        assertEquals(emptyList<Any>(), series.observeSeries().first())
        assertEquals(emptyList<Any>(), books.observeLibraryBooks().first())
    }

    // --- Series defaults and covers ---

    @Test
    fun seriesDetailsComeFromTheFirstVolume() = runTest {
        epub("Series/b.epub", title = "Second", volume = 2.0, author = "Someone Else")
        epub("Series/a.epub", title = "First", volume = 1.0, author = "Yushi Ukai", subjects = listOf("Horror", "Action", "Death game"))

        scanner.scan()

        val details = series.observeSeries().first().single().details
        assertEquals("Yushi Ukai", details.author)
        assertEquals("Nekometaru", details.illustrator)
        assertEquals("About First", details.synopsis)
        assertEquals("Horror", details.mainGenre)
        assertEquals(listOf("Action", "Death game"), details.tags)
        assertNotNull(details.coverPath)
    }

    @Test
    fun aCoverLostFromTheCacheIsReadAgain() = runTest {
        epub("Book.epub")
        scanner.scan()
        val coverPath = db.bookDao().getAll().single().source!!.coverPath!!
        File(coverPath).delete()
        reader.reads.clear()

        scanner.scan()

        assertEquals(listOf(File(library, "Book.epub").path), reader.reads)
        assertTrue(File(coverPath).exists())
    }

    @Test
    fun coversOfBooksThatChangedAreDeleted() = runTest {
        val file = epub("Book.epub")
        scanner.scan()
        val oldCover = File(db.bookDao().getAll().single().source!!.coverPath!!)

        writeFake(file, title = "Changed")
        file.setLastModified(file.lastModified() + 10_000)
        scanner.scan()

        val newCover = File(db.bookDao().getAll().single().source!!.coverPath!!)
        assertFalse(oldCover.exists())
        assertTrue(newCover.exists())
        assertEquals(listOf(newCover.name), coverDir.list()!!.toList())
    }

    @Test
    fun aBookWithoutACoverIsntReadAgain() = runTest {
        epub("Book.epub", withCover = false)
        scanner.scan()
        reader.reads.clear()

        scanner.scan()

        assertNull(db.bookDao().getAll().single().source?.coverPath)
        assertEquals(emptyList<String>(), reader.reads)
    }

    // --- Series cover (#38, D25) ---

    @Test
    fun theSeriesCoverComesFromTheLastVolume() = runTest {
        epub("Series/Vol 2.epub", volume = 2.0, subjects = listOf("Second"))
        epub("Series/Vol 1.epub", volume = 1.0, subjects = listOf("First"))
        epub("Series/Vol 3.epub", volume = 3.0, subjects = listOf("Third"), withCover = false)
        scanner.scan()

        val found = series.getSeriesByFolderPath(File(library, "Series").path)!!
        // Vol 3 has no cover, so Vol 2 is the last volume with one. The other details stay with Vol 1.
        assertEquals(coverOf("Series/Vol 2.epub"), found.details.coverPath)
        assertEquals("First", found.details.mainGenre)
    }

    @Test
    fun aNewLastVolumeBecomesTheSeriesCover() = runTest {
        epub("Series/Vol 1.epub", volume = 1.0)
        scanner.scan()

        epub("Series/Vol 2.epub", volume = 2.0)
        scanner.scan()

        val found = series.getSeriesByFolderPath(File(library, "Series").path)!!
        assertEquals(coverOf("Series/Vol 2.epub"), found.details.coverPath)
    }

    @Test
    fun aPickedCoverStaysUntilItsVolumeChanges() = runTest {
        val first = epub("Series/Vol 1.epub", volume = 1.0)
        epub("Series/Vol 2.epub", volume = 2.0)
        scanner.scan()
        val id = series.getSeriesByFolderPath(File(library, "Series").path)!!.id
        series.updateEdits(id, name = null, details = SeriesDetails(coverPath = coverOf("Series/Vol 1.epub")))

        scanner.scan()
        assertEquals(coverOf("Series/Vol 1.epub"), series.getSeries(id)!!.details.coverPath)

        // A changed file gets a new cover, so the picked one is gone and the default comes back.
        writeFake(first, title = "Changed", volume = 1.0)
        first.setLastModified(first.lastModified() + 10_000)
        scanner.scan()

        val after = series.getSeries(id)!!
        assertNull(after.editedDetails.coverPath)
        assertEquals(coverOf("Series/Vol 2.epub"), after.details.coverPath)
    }

    // --- Helpers ---

    private suspend fun coverOf(relativePath: String): String? =
        db.bookDao().getAll().single { it.filePath == File(library, relativePath).path }.source?.coverPath


    private fun epub(
        relativePath: String,
        title: String = File(relativePath).nameWithoutExtension,
        volume: Double? = null,
        author: String = "Yushi Ukai",
        subjects: List<String> = emptyList(),
        withCover: Boolean = true,
    ): File = File(library, relativePath).also {
        it.parentFile!!.mkdirs()
        writeFake(it, title, volume, author, subjects, withCover)
    }

    private fun writeFake(
        file: File,
        title: String,
        volume: Double? = null,
        author: String = "Yushi Ukai",
        subjects: List<String> = emptyList(),
        withCover: Boolean = true,
    ) {
        file.writeText(
            listOf(title, volume?.toString().orEmpty(), author, subjects.joinToString(","), withCover.toString(), UUID.randomUUID())
                .joinToString("|")
        )
    }

    private class FakeSettings(path: String) : SettingsRepository {
        override val settings: Flow<AppSettings> = MutableStateFlow(AppSettings(libraryFolderPath = path))
        override suspend fun setLibraryFolderPath(path: String) = error("not used")
        override suspend fun resetLibraryFolderPath() = error("not used")
        override suspend fun setHasSeenWelcome() = error("not used")
        override suspend fun setAppTheme(theme: AppTheme) = error("not used")
        override suspend fun setDynamicColor(enabled: Boolean) = error("not used")
        override suspend fun setSeriesSort(sort: SeriesSort) = error("not used")
        override suspend fun setSeriesLayout(layout: SeriesLayout) = error("not used")
        override suspend fun setLibraryGrid(grid: Boolean) = error("not used")
    }

    /** Reads the fields written by [writeFake], and "saves" a cover as a small file. */
    private inner class FakeEpubReader : EpubReader {
        val reads = mutableListOf<String>()

        override suspend fun read(file: File, fingerprint: String): EpubMetadata? {
            reads += file.path
            val fields = file.readText().split("|")
            if (fields[0] == UNREADABLE) return null
            val coverPath = if (fields[4].toBoolean()) {
                File(coverDir, "$fingerprint.jpg").apply { writeText("cover") }.path
            } else {
                null
            }
            return EpubMetadata(
                details = BookDetails(
                    title = fields[0],
                    creators = listOf(Creator(fields[2], "aut"), Creator("Nekometaru", "ill")),
                    description = "About ${fields[0]}",
                    subjects = fields[3].split(",").filter { it.isNotEmpty() }.ifEmpty { null },
                    volumeNumber = fields[1].toDoubleOrNull(),
                    coverPath = coverPath,
                ),
                totalPositions = 100,
            )
        }
    }

    private companion object {
        const val UNREADABLE = "<unreadable>"
    }
}
