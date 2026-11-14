package my.naqibrafiqi.pustaka.data.library.epub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import my.naqibrafiqi.pustaka.data.model.Creator
import my.naqibrafiqi.pustaka.testing.writeEpub
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

/** Opens real (small) EPUB files with Readium. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadiumEpubReaderTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var coverDir: File
    private lateinit var reader: ReadiumEpubReader

    @Before
    fun setUp() {
        coverDir = temp.newFolder("covers")
        reader = ReadiumEpubReader(ReadiumToolkit(ApplicationProvider.getApplicationContext()), CoverStore(coverDir))
    }

    @Test
    fun readsEpub3Details() = runTest {
        val epub = writeEpub(
            temp.root.resolve("Playing Death Games, Vol. 02.epub"),
            metadata = """
                <dc:title>Playing Death Games to Put Food on the Table, Vol. 2</dc:title>
                <dc:creator id="c1">Yushi Ukai</dc:creator>
                <meta refines="#c1" property="role" scheme="marc:relators">aut</meta>
                <dc:creator id="c2">Nekometaru</dc:creator>
                <meta refines="#c2" property="role" scheme="marc:relators">ill</meta>
                <dc:contributor id="c3">Jessica Lange</dc:contributor>
                <meta refines="#c3" property="role" scheme="marc:relators">trl</meta>
                <dc:publisher>Yen On</dc:publisher>
                <dc:date>2024-06-18</dc:date>
                <dc:language>en</dc:language>
                <dc:description>&lt;p&gt;Yuki is &lt;b&gt;back&lt;/b&gt; in the game.&lt;/p&gt;</dc:description>
                <dc:subject>Horror</dc:subject>
                <dc:subject>Action</dc:subject>
                <meta property="belongs-to-collection" id="s1">Playing Death Games</meta>
                <meta refines="#s1" property="collection-type">series</meta>
                <meta refines="#s1" property="group-position">2</meta>
                <meta property="dcterms:modified">2024-06-01T00:00:00Z</meta>
            """.trimIndent(),
            identifier = "urn:isbn:9781975385644",
        )

        val metadata = reader.read(epub, "fp-1")!!
        val details = metadata.details

        assertEquals("Playing Death Games to Put Food on the Table, Vol. 2", details.title)
        assertEquals(
            listOf(Creator("Yushi Ukai", "aut"), Creator("Nekometaru", "ill"), Creator("Jessica Lange", "trl")),
            details.creators,
        )
        assertEquals("Yen On", details.publisher)
        assertEquals("2024-06-18", details.publishedDate)
        assertEquals("urn:isbn:9781975385644", details.identifier)
        assertEquals("en", details.language)
        assertEquals("Yuki is back in the game.", details.description)
        assertEquals(listOf("Horror", "Action"), details.subjects)
        assertEquals("Playing Death Games", details.seriesTitle)
        assertEquals(2.0, details.volumeNumber!!, 0.0)
        assertNull(details.coverPath)
        assertTrue("positions: ${metadata.totalPositions}", (metadata.totalPositions ?: 0) >= 1)
    }

    @Test
    fun readsEpub2RolesAndCalibreSeries() = runTest {
        val epub = writeEpub(
            temp.root.resolve("old.epub"),
            epub3 = false,
            metadata = """
                <dc:title>An Old Book</dc:title>
                <dc:creator opf:role="aut" opf:file-as="Writer, Some">Some Writer</dc:creator>
                <dc:creator opf:role="ill">An Illustrator</dc:creator>
                <dc:contributor>Someone Helpful</dc:contributor>
                <dc:creator>No Role</dc:creator>
                <meta name="calibre:series" content="Old Series"/>
                <meta name="calibre:series_index" content="4.5"/>
            """.trimIndent(),
        )

        val details = reader.read(epub, "fp-2")!!.details

        assertEquals("An Old Book", details.title)
        assertEquals(
            listOf(
                Creator("Some Writer", "aut"),
                Creator("An Illustrator", "ill"),
                Creator("Someone Helpful", OpfCreators.ROLE_CONTRIBUTOR),
                Creator("No Role", null),
            ),
            details.creators,
        )
        assertEquals("Old Series", details.seriesTitle)
        assertEquals(4.5, details.volumeNumber!!, 0.0)
    }

    @Test
    fun savesTheCoverUnderTheFingerprint() = runTest {
        val epub = writeEpub(
            temp.root.resolve("covered.epub"),
            metadata = "<dc:title>With a cover</dc:title>",
            cover = png(width = 60, height = 90),
        )

        val coverPath = reader.read(epub, "abc123")!!.details.coverPath

        assertEquals(File(coverDir, "abc123.jpg").path, coverPath)
        val saved = BitmapFactory.decodeFile(coverPath)
        assertNotNull(saved)
        assertEquals(60, saved.width)
        assertEquals(90, saved.height)
    }

    @Test
    fun missingDetailsAreNull() = runTest {
        val epub = writeEpub(temp.root.resolve("bare.epub"), metadata = "<dc:title>Bare</dc:title>", identifier = null)

        val details = reader.read(epub, "fp-3")!!.details

        assertEquals("Bare", details.title)
        assertNull(details.creators)
        assertNull(details.publisher)
        assertNull(details.publishedDate)
        assertNull(details.description)
        assertNull(details.subjects)
        assertNull(details.seriesTitle)
        assertNull(details.volumeNumber)
    }

    @Test
    fun aFileThatIsntAnEpubGivesNull() = runTest {
        val notAZip = temp.newFile("broken.epub").apply { writeText("this is not a zip file") }
        val zipWithoutOpf = temp.newFile("empty.epub").apply {
            java.util.zip.ZipOutputStream(outputStream()).use { it.putNextEntry(java.util.zip.ZipEntry("hello.txt")) }
        }

        assertNull(reader.read(notAZip, "fp-4"))
        assertNull(reader.read(zipWithoutOpf, "fp-5"))
    }

    private fun png(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }
}
