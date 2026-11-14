package my.naqibrafiqi.pustaka.data.local

import my.naqibrafiqi.pustaka.data.model.Creator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `instants round-trip through epoch millis`() {
        val instant = Instant.parse("2026-11-14T08:30:15.123Z")
        assertEquals(instant, converters.epochMillisToInstant(converters.instantToEpochMillis(instant)))
        assertNull(converters.instantToEpochMillis(null))
        assertNull(converters.epochMillisToInstant(null))
    }

    @Test
    fun `string lists round-trip, including empty ones`() {
        val tags = listOf("Horror", "Death \"Games\"", "日本語")
        assertEquals(tags, converters.jsonToStrings(converters.stringsToJson(tags)))
        assertEquals(emptyList<String>(), converters.jsonToStrings(converters.stringsToJson(emptyList())))
        assertNull(converters.stringsToJson(null))
    }

    @Test
    fun `creators round-trip with and without roles`() {
        val creators = listOf(Creator("Yushi Ukai", "aut"), Creator("Anonymous"))
        assertEquals(creators, converters.jsonToCreators(converters.creatorsToJson(creators)))
    }
}
