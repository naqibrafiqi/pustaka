package my.naqibrafiqi.pustaka.data.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatBytesTest {

    @Test
    fun usesTheLargestUnitThatFits() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("999 B", formatBytes(999))
        assertEquals("1.0 KB", formatBytes(1_000))
        assertEquals("12.3 KB", formatBytes(12_345))
        assertEquals("512 KB", formatBytes(512_000))
        assertEquals("4.5 MB", formatBytes(4_500_000))
        assertEquals("1.2 GB", formatBytes(1_234_000_000))
    }
}
