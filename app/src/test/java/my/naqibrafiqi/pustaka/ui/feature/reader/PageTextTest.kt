package my.naqibrafiqi.pustaka.ui.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PageTextTest {

    @Test
    fun readsTheScriptsResult() {
        assertEquals("It began \"here\".", PageText.parseResult("\"It began \\\"here\\\".\""))
        assertNull(PageText.parseResult("null"))
        assertNull(PageText.parseResult("\"  \""))
    }
}
