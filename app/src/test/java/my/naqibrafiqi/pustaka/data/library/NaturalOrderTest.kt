package my.naqibrafiqi.pustaka.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderTest {

    private fun sorted(vararg names: String) = names.sortedWith(NaturalOrder)

    @Test
    fun `10 comes after 9`() {
        assertEquals(
            listOf("Vol 1", "Vol 2", "Vol 9", "Vol 10", "Vol 11"),
            sorted("Vol 10", "Vol 2", "Vol 11", "Vol 1", "Vol 9"),
        )
    }

    @Test
    fun `leading zeros don't change the number`() {
        assertEquals(listOf("Vol 002", "Vol 3", "Vol 010"), sorted("Vol 010", "Vol 3", "Vol 002"))
    }

    @Test
    fun `letters are compared without regard to case`() {
        assertEquals(listOf("alpha", "Beta", "gamma"), sorted("gamma", "Beta", "alpha"))
    }

    @Test
    fun `a shorter name comes first when the start is the same`() {
        assertEquals(listOf("Book", "Book 2", "Book Two"), sorted("Book Two", "Book 2", "Book"))
    }

    @Test
    fun `numbers longer than a Long still sort correctly`() {
        assertEquals(
            listOf("x 99999999999999999999", "x 100000000000000000000"),
            sorted("x 100000000000000000000", "x 99999999999999999999"),
        )
    }

    @Test
    fun `names that are equal apart from zeros or case still get a fixed order`() {
        assertTrue(NaturalOrder.compare("Vol 01", "Vol 1") != 0)
        assertEquals(0, NaturalOrder.compare("Vol 1", "Vol 1"))
        assertEquals(
            -NaturalOrder.compare("vol 1", "Vol 1").coerceIn(-1, 1),
            NaturalOrder.compare("Vol 1", "vol 1").coerceIn(-1, 1),
        )
    }
}
