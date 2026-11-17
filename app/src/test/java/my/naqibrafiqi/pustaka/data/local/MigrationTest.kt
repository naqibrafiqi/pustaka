package my.naqibrafiqi.pustaka.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.local.entity.BookmarkEntity
import my.naqibrafiqi.pustaka.data.model.ReadingStatus
import my.naqibrafiqi.pustaka.testing.T0
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Checks that a database from 1.0.0 opens in this version with everything still in it.
 *
 * The old database is built from the schema Room exported for it, in `app/schemas/`. Room checks
 * the tables against this version's when it opens the database after migrating, so a migration
 * that leaves something out fails here.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var file: File

    @Before
    fun setUp() {
        file = context.getDatabasePath(NAME)
        file.parentFile?.mkdirs()
        context.deleteDatabase(NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(NAME)
    }

    @Test
    fun version1To2KeepsTheLibraryAndAddsBookmarks() = runTest {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            createFromSchema(db, version = 1)
            db.execSQL(
                "INSERT INTO books (id, filePath, fileName, fingerprint, fileSize, fileLastModified, isMissing, isHidden, readingStatus, addedAt) " +
                    "VALUES (1, '/lib/a.epub', 'a.epub', 'f', 10, 0, 0, 0, 'READING', 0)"
            )
            db.execSQL("INSERT INTO highlights (bookId, locator, text, color, createdAt, updatedAt) VALUES (1, '{}', 'quote', 0, 0, 0)")
            db.execSQL("INSERT INTO reading_progress (bookId, locator, progression, updatedAt) VALUES (1, '{}', 0.5, 0)")
        }

        val database = Room.databaseBuilder(context, PustakaDatabase::class.java, NAME).allowMainThreadQueries().build()
        try {
            assertEquals(ReadingStatus.READING, database.bookDao().getById(1)?.readingStatus)
            assertEquals("quote", database.highlightDao().observeForBook(1).first().single().text)
            assertEquals(0.5, database.readingProgressDao().get(1)?.progression)

            database.bookmarkDao().insert(BookmarkEntity(bookId = 1, locator = "{}", progression = 0.5, text = "Once", createdAt = T0))
            assertEquals(1, database.bookmarkDao().observeForBook(1).first().size)
            // Bookmarks go with their book.
            database.bookDao().deleteById(1)
            assertTrue(database.bookmarkDao().observeForBook(1).first().isEmpty())
        } finally {
            database.close()
        }
    }

    /** Makes the tables of [version] as Room exported them, the way Room itself would have. */
    private fun createFromSchema(db: SQLiteDatabase, version: Int) {
        // Gradle runs the unit tests from the module's folder.
        val json = File("schemas/${PustakaDatabase::class.java.name}/$version.json").readText()
        val schema = JSONObject(json).getJSONObject("database")
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace(TABLE_NAME, table))
            val indices = entity.optJSONArray("indices") ?: continue
            for (j in 0 until indices.length()) {
                db.execSQL(indices.getJSONObject(j).getString("createSql").replace(TABLE_NAME, table))
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        db.version = version
    }

    private companion object {
        const val NAME = "migration-test.db"
        const val TABLE_NAME = "\${TABLE_NAME}"
    }
}
