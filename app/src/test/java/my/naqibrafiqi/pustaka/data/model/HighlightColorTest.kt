package my.naqibrafiqi.pustaka.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HighlightColorTest {

    @Test
    fun aSavedColourIsReadBack() {
        HighlightColor.entries.forEach { assertEquals(it, HighlightColor.of(it.argb)) }
    }

    @Test
    fun aColourThisVersionDoesntOfferBecomesYellow() {
        assertEquals(HighlightColor.YELLOW, HighlightColor.of(0xFF123456.toInt()))
        assertEquals(HighlightColor.YELLOW, HighlightColor.of(0))
    }
}
