package my.naqibrafiqi.pustaka.data.library.scan

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import my.naqibrafiqi.pustaka.data.library.Fingerprint
import my.naqibrafiqi.pustaka.data.library.SeriesDefaults
import my.naqibrafiqi.pustaka.data.library.epub.CoverStore
import my.naqibrafiqi.pustaka.data.library.epub.EpubMetadata
import my.naqibrafiqi.pustaka.data.library.epub.EpubReader
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.local.entity.BookEntity
import my.naqibrafiqi.pustaka.data.local.entity.SeriesEntity
import my.naqibrafiqi.pustaka.data.local.toModel
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import java.io.File
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Brings the database up to date with the library folder. See `docs/local_storage_and_library.md`.
 *
 * A scan has two steps:
 * 1. Look at the folder. Only files that are new, or whose size or last-modified time changed, are
 *    fingerprinted and opened, so a scan with nothing new is quick.
 * 2. Save everything in one database transaction, so the screens update once.
 *
 * The scanner only reads the folder. It never renames, moves or changes the user's files.
 */
@Singleton
class LibraryScanner @Inject constructor(
    private val database: PustakaDatabase,
    private val settings: SettingsRepository,
    private val epubReader: EpubReader,
    private val coverStore: CoverStore,
    private val clock: Clock,
) {
    private val mutex = Mutex()

    /** Scans the library folder from the settings. Scans never overlap: a second one waits. */
    suspend fun scan(): ScanResult = mutex.withLock {
        val folder = File(settings.settings.first().libraryFolderPath)
        withContext(Dispatchers.IO) { scan(folder) }
    }

    private suspend fun scan(root: File): ScanResult {
        val listing = LibraryListing.of(root) ?: return ScanResult.FolderUnavailable(root.path)

        val knownBooks = database.bookDao().getAll()
        val knownSeries = database.seriesDao().getAll()
        val booksByPath = knownBooks.associateBy { it.filePath }
        val foundPaths = listing.files.mapTo(HashSet()) { it.path }

        // Books whose file isn't where it was. Unless a file with the same fingerprint turns up
        // somewhere else (the book was moved or renamed), they're marked missing.
        val gone = knownBooks.filter {
            it.filePath !in foundPaths && File(it.filePath).parent !in listing.unreadableFolders
        }
        val goneByFingerprint = gone.groupByTo(HashMap()) { it.fingerprint }

        val plans = listing.files.map { found -> plan(found, booksByPath[found.path], goneByFingerprint) }
        val seriesPlans = planSeries(listing, knownSeries, plans)
        val stillGone = goneByFingerprint.values.flatten()

        return database.withTransaction {
            save(seriesPlans, plans, stillGone)
        }.also {
            deleteUnusedCovers()
        }
    }

    // --- Step 1: look at the folder ---

    private sealed interface BookPlan {
        val found: FoundFile
        val size: Long
        val lastModified: Instant

        /** A known file that hasn't changed. */
        data class Unchanged(
            override val found: FoundFile,
            override val size: Long,
            override val lastModified: Instant,
            val book: BookEntity,
        ) : BookPlan

        /** A known file that changed, or whose cover has gone from the cache, so it's read again. */
        data class Changed(
            override val found: FoundFile,
            override val size: Long,
            override val lastModified: Instant,
            val book: BookEntity,
            val fingerprint: String,
            val metadata: EpubMetadata?,
        ) : BookPlan

        /** A known book under a new path. [metadata] is only read if its cover has gone. */
        data class Moved(
            override val found: FoundFile,
            override val size: Long,
            override val lastModified: Instant,
            val book: BookEntity,
            val metadata: EpubMetadata?,
        ) : BookPlan

        data class New(
            override val found: FoundFile,
            override val size: Long,
            override val lastModified: Instant,
            val fingerprint: String,
            val metadata: EpubMetadata?,
        ) : BookPlan
    }

    private suspend fun plan(
        found: FoundFile,
        known: BookEntity?,
        goneByFingerprint: MutableMap<String, MutableList<BookEntity>>,
    ): BookPlan {
        val size = found.file.length()
        val lastModified = Instant.ofEpochMilli(found.file.lastModified())

        if (known != null) {
            val isUnchanged = known.fileSize == size && known.fileLastModified == lastModified
            return when {
                isUnchanged && !known.hasLostCover() ->
                    BookPlan.Unchanged(found, size, lastModified, known)
                isUnchanged ->
                    BookPlan.Changed(found, size, lastModified, known, known.fingerprint, read(found, known.fingerprint))
                else -> {
                    val fingerprint = Fingerprint.of(found.file)
                    BookPlan.Changed(found, size, lastModified, known, fingerprint, read(found, fingerprint))
                }
            }
        }

        val fingerprint = Fingerprint.of(found.file)
        val candidates = goneByFingerprint[fingerprint]
        val moved = candidates?.removeAt(0)
        if (candidates != null && candidates.isEmpty()) goneByFingerprint.remove(fingerprint)
        if (moved != null) {
            val metadata = if (moved.hasLostCover()) read(found, fingerprint) else null
            return BookPlan.Moved(found, size, lastModified, moved, metadata)
        }
        return BookPlan.New(found, size, lastModified, fingerprint, read(found, fingerprint))
    }

    private suspend fun read(found: FoundFile, fingerprint: String): EpubMetadata? =
        runCatching { epubReader.read(found.file, fingerprint) }.getOrNull()

    /** True when the book had a cover and the cache has lost it. */
    private fun BookEntity.hasLostCover(): Boolean =
        source?.coverPath?.let { !File(it).exists() } ?: false

    private sealed interface SeriesPlan {
        val folder: File

        data class Existing(override val folder: File, val id: Long) : SeriesPlan

        /** A known series whose folder was renamed: its books turned up in [folder]. */
        data class Renamed(override val folder: File, val id: Long) : SeriesPlan

        data class New(override val folder: File) : SeriesPlan
    }

    private class SeriesPlans(val folders: List<SeriesPlan>, val gone: List<SeriesEntity>)

    private fun planSeries(
        listing: LibraryListing,
        knownSeries: List<SeriesEntity>,
        plans: List<BookPlan>,
    ): SeriesPlans {
        val knownByPath = knownSeries.associateBy { it.folderPath }
        val foundFolderPaths = listing.seriesFolders.mapTo(HashSet()) { it.path }
        val gone = knownSeries.filter { it.folderPath !in foundFolderPaths }.associateByTo(LinkedHashMap()) { it.id }

        val folders = listing.seriesFolders.map { folder ->
            knownByPath[folder.path]?.let { return@map SeriesPlan.Existing(folder, it.id) }
            val previousId = plans.asSequence()
                .filterIsInstance<BookPlan.Moved>()
                .filter { it.found.seriesFolder == folder }
                .mapNotNull { it.book.seriesId }
                .firstOrNull { it in gone }
            if (previousId != null) {
                gone.remove(previousId)
                SeriesPlan.Renamed(folder, previousId)
            } else {
                SeriesPlan.New(folder)
            }
        }
        return SeriesPlans(folders, gone.values.toList())
    }

    // --- Step 2: save ---

    private suspend fun save(
        seriesPlans: SeriesPlans,
        plans: List<BookPlan>,
        stillGone: List<BookEntity>,
    ): ScanResult.Success {
        val bookDao = database.bookDao()
        val seriesDao = database.seriesDao()
        val now = clock.instant()

        var newSeries = 0
        val seriesIdByPath = HashMap<String, Long>()
        for (plan in seriesPlans.folders) {
            seriesIdByPath[plan.folder.path] = when (plan) {
                is SeriesPlan.Existing -> plan.id
                is SeriesPlan.Renamed -> {
                    seriesDao.getById(plan.id)?.let {
                        seriesDao.update(it.copy(folderPath = plan.folder.path, folderName = plan.folder.name, updatedAt = now))
                    }
                    plan.id
                }
                is SeriesPlan.New -> {
                    newSeries++
                    seriesDao.insert(
                        SeriesEntity(
                            folderPath = plan.folder.path,
                            folderName = plan.folder.name,
                            editedName = null,
                            source = null,
                            edited = null,
                            isHidden = false,
                            createdAt = now,
                            updatedAt = now,
                        )
                    )
                }
            }
        }

        var newBooks = 0
        var movedBooks = 0
        var updatedBooks = 0
        var restoredBooks = 0
        val unreadable = mutableListOf<String>()

        for (plan in plans) {
            val seriesId = plan.found.seriesFolder?.let { seriesIdByPath.getValue(it.path) }
            when (plan) {
                is BookPlan.Unchanged -> {
                    if (!plan.book.isMissing && plan.book.seriesId == seriesId) continue
                    if (plan.book.isMissing) restoredBooks++
                    // Read again, so edits made while the scan was running are kept.
                    val current = bookDao.getById(plan.book.id) ?: continue
                    bookDao.update(current.copy(seriesId = seriesId, isMissing = false))
                }
                is BookPlan.Changed -> {
                    if (plan.book.isMissing) restoredBooks++ else updatedBooks++
                    if (plan.metadata == null) unreadable += plan.found.path
                    val current = bookDao.getById(plan.book.id) ?: continue
                    bookDao.update(
                        current.copy(
                            seriesId = seriesId,
                            fingerprint = plan.fingerprint,
                            fileSize = plan.size,
                            fileLastModified = plan.lastModified,
                            isMissing = false,
                        ).withMetadata(plan.metadata)
                    )
                }
                is BookPlan.Moved -> {
                    if (plan.book.isMissing) restoredBooks++ else movedBooks++
                    val current = bookDao.getById(plan.book.id) ?: continue
                    bookDao.update(
                        current.copy(
                            seriesId = seriesId,
                            filePath = plan.found.path,
                            fileName = plan.found.file.name,
                            fileSize = plan.size,
                            fileLastModified = plan.lastModified,
                            isMissing = false,
                        ).withMetadata(plan.metadata)
                    )
                }
                is BookPlan.New -> {
                    newBooks++
                    if (plan.metadata == null) unreadable += plan.found.path
                    bookDao.insert(
                        BookEntity(
                            seriesId = seriesId,
                            filePath = plan.found.path,
                            fileName = plan.found.file.name,
                            fingerprint = plan.fingerprint,
                            fileSize = plan.size,
                            fileLastModified = plan.lastModified,
                            isMissing = false,
                            isHidden = false,
                            editedName = null,
                            source = plan.metadata?.details,
                            edited = null,
                            totalPositions = plan.metadata?.totalPositions,
                            readingStatus = ReadingStatus.UNREAD,
                            addedAt = now,
                            lastOpenedAt = null,
                        )
                    )
                }
            }
        }

        var missingBooks = 0
        for (book in stillGone) {
            if (book.isMissing) continue
            bookDao.setMissing(book.id, true)
            missingBooks++
        }

        var removedSeries = 0
        for (series in seriesPlans.gone) {
            if (bookDao.countInSeries(series.id) == 0) {
                seriesDao.deleteById(series.id)
                removedSeries++
            }
        }

        for (series in seriesDao.getAll()) {
            val defaults = SeriesDefaults.fromVolumes(bookDao.getInSeries(series.id).map { it.toModel() })
            if (series.toModel().sourceDetails != defaults) {
                seriesDao.update(series.copy(source = defaults, updatedAt = now))
            }
        }

        return ScanResult.Success(
            newBooks = newBooks,
            movedBooks = movedBooks,
            updatedBooks = updatedBooks,
            restoredBooks = restoredBooks,
            missingBooks = missingBooks,
            newSeries = newSeries,
            removedSeries = removedSeries,
            unreadableFiles = unreadable,
        )
    }

    /** A file that couldn't be read keeps the details it had. */
    private fun BookEntity.withMetadata(metadata: EpubMetadata?): BookEntity =
        if (metadata == null) {
            this
        } else {
            copy(source = metadata.details, totalPositions = metadata.totalPositions ?: totalPositions)
        }

    private suspend fun deleteUnusedCovers() {
        val inUse = database.bookDao().getAll().flatMapTo(HashSet()) {
            listOfNotNull(it.source?.coverPath, it.edited?.coverPath)
        }
        coverStore.deleteAllExcept(inUse)
    }
}
