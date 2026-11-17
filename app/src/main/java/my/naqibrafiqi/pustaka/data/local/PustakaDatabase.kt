package my.naqibrafiqi.pustaka.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import my.naqibrafiqi.pustaka.data.local.dao.BookDao
import my.naqibrafiqi.pustaka.data.local.dao.BookmarkDao
import my.naqibrafiqi.pustaka.data.local.dao.HighlightDao
import my.naqibrafiqi.pustaka.data.local.dao.ReadingProgressDao
import my.naqibrafiqi.pustaka.data.local.dao.ReadingSessionDao
import my.naqibrafiqi.pustaka.data.local.dao.SeriesDao
import my.naqibrafiqi.pustaka.data.local.entity.BookEntity
import my.naqibrafiqi.pustaka.data.local.entity.BookmarkEntity
import my.naqibrafiqi.pustaka.data.local.entity.HighlightEntity
import my.naqibrafiqi.pustaka.data.local.entity.ReadingProgressEntity
import my.naqibrafiqi.pustaka.data.local.entity.ReadingSessionEntity
import my.naqibrafiqi.pustaka.data.local.entity.SeriesEntity

/**
 * The schema of each version is exported to `app/schemas/` so migrations can be checked.
 *
 * - Version 1: 1.0.0.
 * - Version 2: adds the `bookmarks` table (#37). Nothing else changes, so Room migrates it.
 */
@Database(
    entities = [
        SeriesEntity::class,
        BookEntity::class,
        ReadingProgressEntity::class,
        ReadingSessionEntity::class,
        HighlightEntity::class,
        BookmarkEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class PustakaDatabase : RoomDatabase() {
    abstract fun seriesDao(): SeriesDao
    abstract fun bookDao(): BookDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun highlightDao(): HighlightDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        const val NAME = "pustaka.db"
    }
}
