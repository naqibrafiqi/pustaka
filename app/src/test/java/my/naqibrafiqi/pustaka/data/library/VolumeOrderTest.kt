package my.naqibrafiqi.pustaka.data.library

import my.naqibrafiqi.pustaka.testing.book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VolumeOrderTest {

    // --- Number in the filename ---

    @Test
    fun `finds the number after Vol, Volume, v and #`() {
        val cases = mapOf(
            "Playing Death Games, Vol. 04.epub" to 4.0,
            "Playing Death Games Vol 4.epub" to 4.0,
            "Playing Death Games Vol.12.epub" to 12.0,
            "Playing Death Games Volume 4.epub" to 4.0,
            "Playing Death Games VOLUME 7.EPUB" to 7.0,
            "[Group] Series v04 (Digital).epub" to 4.0,
            "Series #4.epub" to 4.0,
            "Series Vol. 4.5.epub" to 4.5,
        )
        cases.forEach { (fileName, expected) ->
            assertEquals(fileName, expected, VolumeOrder.numberInFileName(fileName))
        }
    }

    @Test
    fun `falls back to a number at the end of the name`() {
        assertEquals(3.0, VolumeOrder.numberInFileName("Playing Death Games, 3.epub"))
        assertEquals(10.0, VolumeOrder.numberInFileName("Book 10.epub"))
        assertEquals(12.0, VolumeOrder.numberInFileName("Chapter-12.epub"))
    }

    @Test
    fun `skips tags at the end of the name`() {
        assertEquals(5.0, VolumeOrder.numberInFileName("Series 05 (Digital) [1080p].epub"))
    }

    @Test
    fun `the marked number wins over a number at the end`() {
        assertEquals(2.0, VolumeOrder.numberInFileName("Series Vol. 2 Part 1.epub"))
    }

    @Test
    fun `v inside a word isn't a volume marker`() {
        // "Rev" and "Revolution" contain a "v" but aren't followed by the volume number.
        assertEquals(9.0, VolumeOrder.numberInFileName("Rev 3 Revolution 9.epub"))
    }

    @Test
    fun `returns null when there is no number`() {
        assertNull(VolumeOrder.numberInFileName("The Left Hand of Darkness.epub"))
        assertNull(VolumeOrder.numberInFileName("Series (2019).epub"))
    }

    // --- Which rule gives the number ---

    @Test
    fun `the user's number comes first, then the EPUB's, then the filename's`() {
        assertEquals(7.0, VolumeOrder.volumeNumberOf(7.0, 2.0, "Vol 3.epub"))
        assertEquals(2.0, VolumeOrder.volumeNumberOf(null, 2.0, "Vol 3.epub"))
        assertEquals(3.0, VolumeOrder.volumeNumberOf(null, null, "Vol 3.epub"))
        assertNull(VolumeOrder.volumeNumberOf(null, null, "No number.epub"))
    }

    @Test
    fun `a book's volume number uses its edited and source details`() {
        assertEquals(7.0, book("Vol 3.epub", volumeInFile = 2.0, editedVolume = 7.0).volumeNumber)
        assertEquals(2.0, book("Vol 3.epub", volumeInFile = 2.0).volumeNumber)
        assertEquals(3.0, book("Vol 3.epub").volumeNumber)
    }

    // --- Sorting ---

    private fun List<my.naqibrafiqi.pustaka.data.model.Book>.names() = map { it.fileName }

    @Test
    fun `sorts by filename number with 10 after 9`() {
        val books = listOf("Series 10.epub", "Series 2.epub", "Series 9.epub", "Series 1.epub").map { book(it) }
        assertEquals(
            listOf("Series 1.epub", "Series 2.epub", "Series 9.epub", "Series 10.epub"),
            VolumeOrder.sort(books).names(),
        )
    }

    @Test
    fun `the EPUB's number overrides the filename`() {
        val books = listOf(
            book("A Vol 1.epub", volumeInFile = 3.0),
            book("B Vol 2.epub"),
            book("C Vol 3.epub", volumeInFile = 1.0),
        )
        assertEquals(listOf("C Vol 3.epub", "B Vol 2.epub", "A Vol 1.epub"), VolumeOrder.sort(books).names())
    }

    @Test
    fun `the user's number overrides everything`() {
        val books = listOf(
            book("Vol 1.epub", volumeInFile = 1.0, editedVolume = 5.0),
            book("Vol 2.epub", volumeInFile = 2.0),
        )
        assertEquals(listOf("Vol 2.epub", "Vol 1.epub"), VolumeOrder.sort(books).names())
    }

    @Test
    fun `books with the same number are ordered by filename`() {
        val books = listOf(
            book("Series 10 Extra.epub", volumeInFile = 1.0),
            book("Series 9 Extra.epub", volumeInFile = 1.0),
        )
        assertEquals(listOf("Series 9 Extra.epub", "Series 10 Extra.epub"), VolumeOrder.sort(books).names())
    }

    @Test
    fun `books without a number come last, sorted by filename`() {
        val books = listOf(
            book("Side Story.epub"),
            book("Afterword.epub"),
            book("Vol 2.epub"),
            book("Vol 1.epub"),
        )
        assertEquals(
            listOf("Vol 1.epub", "Vol 2.epub", "Afterword.epub", "Side Story.epub"),
            VolumeOrder.sort(books).names(),
        )
    }

    @Test
    fun `decimal volumes sit between whole ones`() {
        val books = listOf(book("Vol 2.epub"), book("Vol 1.5.epub"), book("Vol 1.epub"))
        assertEquals(listOf("Vol 1.epub", "Vol 1.5.epub", "Vol 2.epub"), VolumeOrder.sort(books).names())
    }
}
