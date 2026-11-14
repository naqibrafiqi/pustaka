package my.naqibrafiqi.pustaka.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import my.naqibrafiqi.pustaka.data.local.entity.SeriesEntity
import java.time.Instant

@Dao
interface SeriesDao {

    @Query("SELECT * FROM series WHERE isHidden = 0")
    fun observeVisible(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE isHidden = 1")
    fun observeHidden(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE id = :id")
    fun observeById(id: Long): Flow<SeriesEntity?>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getById(id: Long): SeriesEntity?

    @Query("SELECT * FROM series WHERE folderPath = :folderPath")
    suspend fun getByFolderPath(folderPath: String): SeriesEntity?

    @Query("SELECT * FROM series")
    suspend fun getAll(): List<SeriesEntity>

    @Insert
    suspend fun insert(series: SeriesEntity): Long

    @Update
    suspend fun update(series: SeriesEntity)

    @Query("UPDATE series SET isHidden = :hidden, updatedAt = :now WHERE id = :id")
    suspend fun setHidden(id: Long, hidden: Boolean, now: Instant)

    /** Also deletes the series' books, because of the foreign key on `books.seriesId`. */
    @Query("DELETE FROM series WHERE id = :id")
    suspend fun deleteById(id: Long)
}
