package my.naqibrafiqi.pustaka.data.model

import my.naqibrafiqi.pustaka.testing.book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The "value from the file, or the user's edit" rules. */
class EditableTest {

    @Test
    fun `the edit wins over the default`() {
        val name = Editable(default = "From file", edited = "Mine")
        assertEquals("Mine", name.value)
        assertTrue(name.isEdited)
    }

    @Test
    fun `without an edit the default is used`() {
        val name = Editable(default = "From file")
        assertEquals("From file", name.value)
        assertFalse(name.isEdited)
    }

    @Test
    fun `text edits are trimmed`() {
        assertEquals("Mine", EditRules.text("  Mine  ", "From file"))
    }

    @Test
    fun `a blank text edit resets to the default`() {
        assertNull(EditRules.text("   ", "From file"))
        assertNull(EditRules.text(null, "From file"))
    }

    @Test
    fun `an edit that matches the default isn't stored`() {
        assertNull(EditRules.text("From file ", "From file"))
        assertNull(EditRules.value(2.0, 2.0))
        assertNull(EditRules.list(listOf("Horror", "Thriller"), listOf("Horror", "Thriller")))
    }

    @Test
    fun `an edit is stored when there's no default`() {
        assertEquals("Mine", EditRules.text("Mine", null))
        assertEquals(3.0, EditRules.value(3.0, null))
    }

    @Test
    fun `list edits drop blank and repeated entries`() {
        assertEquals(listOf("Horror", "Thriller"), EditRules.list(listOf(" Horror", "", "Thriller", "Horror"), null))
        assertNull(EditRules.list(listOf(" ", ""), listOf("Horror")))
    }

    @Test
    fun `creator edits drop blank names and empty roles`() {
        val edited = EditRules.creators(
            listOf(Creator(" Yushi Ukai ", " "), Creator("  ", "ill"), Creator("Nekometaru", "ill")),
            default = null,
        )
        assertEquals(listOf(Creator("Yushi Ukai", null), Creator("Nekometaru", "ill")), edited)
    }

    // --- Book details ---

    private val fromFile = BookDetails(
        title = "Title in file",
        creators = listOf(Creator("Yushi Ukai", "aut"), Creator("Nekometaru", "ill")),
        publisher = "Yen On",
        subjects = listOf("Horror"),
        volumeNumber = 1.0,
    )

    @Test
    fun `book details overlay edits on the file's values field by field`() {
        val edits = BookDetails(title = "My title", subjects = listOf("Thriller"))
        val shown = edits.overlaidOn(fromFile)
        assertEquals("My title", shown.title)
        assertEquals(listOf("Thriller"), shown.subjects)
        assertEquals("Yen On", shown.publisher)
        assertEquals(1.0, shown.volumeNumber)
        assertEquals(fromFile.creators, shown.creators)
    }

    @Test
    fun `saving the form only keeps the fields that changed`() {
        val form = fromFile.copy(title = "My title", publisher = " Yen On ", description = "")
        val edits = form.editsAgainst(fromFile)
        assertEquals(BookDetails(title = "My title"), edits)
    }

    @Test
    fun `saving the form unchanged stores no edits`() {
        assertEquals(BookDetails.EMPTY, fromFile.editsAgainst(fromFile))
    }

    @Test
    fun `authors are creators with no role or the author role`() {
        val details = BookDetails(
            creators = listOf(Creator("A"), Creator("B", "aut"), Creator("C", "ill"), Creator("D", "trl")),
        )
        assertEquals(listOf("A", "B"), details.authors)
        assertEquals(listOf("C"), details.illustrators)
        assertEquals(listOf("D"), details.translators)
    }

    @Test
    fun `a book's name defaults to the filename without the extension`() {
        assertEquals("Playing Death Games, Vol. 01", book("Playing Death Games, Vol. 01.epub").name.value)
        assertEquals("Shouting", book("Shouting.EPUB").name.value)
        assertEquals("Mine", book("File.epub").copy(editedName = "Mine").name.value)
    }

    // --- Series details ---

    @Test
    fun `series details overlay and save the same way`() {
        val defaults = SeriesDetails(author = "Yushi Ukai", tags = listOf("Horror"))
        val edits = SeriesDetails(author = "Yushi Ukai", synopsis = " New synopsis ", tags = emptyList())
            .editsAgainst(defaults)
        assertEquals(SeriesDetails(synopsis = "New synopsis"), edits)
        assertEquals(
            SeriesDetails(author = "Yushi Ukai", synopsis = "New synopsis", tags = listOf("Horror")),
            edits.overlaidOn(defaults),
        )
    }
}
