package my.naqibrafiqi.pustaka.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import my.naqibrafiqi.pustaka.data.local.dao.BookDao
import my.naqibrafiqi.pustaka.data.local.dao.HighlightDao
import my.naqibrafiqi.pustaka.data.local.dao.ReadingProgressDao
import my.naqibrafiqi.pustaka.data.local.dao.ReadingSessionDao
import my.naqibrafiqi.pustaka.data.local.dao.SeriesDao
import my.naqibrafiqi.pustaka.data.local.entity.BookEntity
import my.naqibrafiqi.pustaka.data.local.entity.HighlightEntity
import my.naqibrafiqi.pustaka.data.local.entity.ReadingProgressEntity
import my.naqibrafiqi.pustaka.data.local.entity.ReadingSessionEntity
import my.naqibrafiqi.pustaka.data.local.entity.SeriesEntity

/** The schema of each version is exported to `app/schemas/` so migrations can be checked. */
@Database(
    entities = [
        SeriesEntity::class,
        BookEntity::class,
        ReadingProgressEntity::class,
        ReadingSessionEntity::class,
        HighlightEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PustakaDatabase : RoomDatabase() {
    abstract fun seriesDao(): SeriesDao
    abstract fun bookDao(): BookDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun highlightDao(): HighlightDao

    companion object {
        const val NAME = "pustaka.db"
    }
}
