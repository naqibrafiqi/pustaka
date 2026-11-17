package my.naqibrafiqi.pustaka.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import my.naqibrafiqi.pustaka.data.library.NaturalOrder
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.local.dao.SeriesDao
import my.naqibrafiqi.pustaka.data.local.entity.SeriesEntity
import my.naqibrafiqi.pustaka.data.local.toEntity
import my.naqibrafiqi.pustaka.data.local.toModel
import my.naqibrafiqi.pustaka.data.model.EditRules
import my.naqibrafiqi.pustaka.data.model.Series
import my.naqibrafiqi.pustaka.data.model.SeriesDetails
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

interface SeriesRepository {

    /** Series that aren't hidden, sorted by name. */
    fun observeSeries(): Flow<List<Series>>

    fun observeSeries(id: Long): Flow<Series?>

    /** Series removed from the library, sorted by name. */
    fun observeHiddenSeries(): Flow<List<Series>>

    suspend fun getSeries(id: Long): Series?

    suspend fun getSeriesByFolderPath(folderPath: String): Series?

    /** Adds a series for a folder. Returns its id. */
    suspend fun addSeries(
        folderPath: String,
        folderName: String,
        sourceDetails: SeriesDetails = SeriesDetails.EMPTY,
    ): Long

    /** Replaces the default details, for example after the first or last volume has changed. */
    suspend fun updateSourceDetails(id: Long, sourceDetails: SeriesDetails)

    /**
     * Saves the values from the Edit details form. Values that are blank or match the default
     * aren't stored as edits, so those fields go back to their defaults.
     */
    suspend fun updateEdits(id: Long, name: String?, details: SeriesDetails)

    /** Removes all of the user's edits. */
    suspend fun resetEdits(id: Long)

    /** Hides or restores a series ("Remove from library"). Its folder isn't touched. */
    suspend fun setHidden(id: Long, hidden: Boolean)

    /** Deletes the series and all of its books from the database. The folder isn't touched. */
    suspend fun deleteSeries(id: Long)
}

class DefaultSeriesRepository @Inject constructor(
    private val database: PustakaDatabase,
    private val clock: Clock,
) : SeriesRepository {

    private val dao: SeriesDao = database.seriesDao()

    override fun observeSeries(): Flow<List<Series>> =
        dao.observeVisible().map { it.toSortedModels() }

    override fun observeSeries(id: Long): Flow<Series?> =
        dao.observeById(id).map { it?.toModel() }

    override fun observeHiddenSeries(): Flow<List<Series>> =
        dao.observeHidden().map { it.toSortedModels() }

    override suspend fun getSeries(id: Long): Series? = dao.getById(id)?.toModel()

    override suspend fun getSeriesByFolderPath(folderPath: String): Series? =
        dao.getByFolderPath(folderPath)?.toModel()

    override suspend fun addSeries(
        folderPath: String,
        folderName: String,
        sourceDetails: SeriesDetails,
    ): Long {
        val now = now()
        return dao.insert(
            Series(
                folderPath = folderPath,
                folderName = folderName,
                sourceDetails = sourceDetails,
                createdAt = now,
                updatedAt = now,
            ).toEntity()
        )
    }

    override suspend fun updateSourceDetails(id: Long, sourceDetails: SeriesDetails) =
        modify(id) { it.copy(sourceDetails = sourceDetails) }

    override suspend fun updateEdits(id: Long, name: String?, details: SeriesDetails) =
        modify(id) { series ->
            series.copy(
                editedName = EditRules.text(name, series.folderName),
                editedDetails = details.editsAgainst(series.sourceDetails),
            )
        }

    override suspend fun resetEdits(id: Long) =
        modify(id) { it.copy(editedName = null, editedDetails = SeriesDetails.EMPTY) }

    override suspend fun setHidden(id: Long, hidden: Boolean) = dao.setHidden(id, hidden, now())

    override suspend fun deleteSeries(id: Long) = dao.deleteById(id)

    private suspend fun modify(id: Long, change: (Series) -> Series) {
        database.withTransaction {
            val series = dao.getById(id)?.toModel() ?: return@withTransaction
            dao.update(change(series).copy(updatedAt = now()).toEntity())
        }
    }

    private fun now(): Instant = clock.instant()

    private fun List<SeriesEntity>.toSortedModels() =
        map { it.toModel() }.sortedWith(compareBy(NaturalOrder) { it.name.value })
}
