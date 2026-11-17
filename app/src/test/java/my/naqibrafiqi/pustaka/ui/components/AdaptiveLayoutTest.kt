package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How many columns each layout gets. Widths are the space for the books after the screen's 16dp
 * sides: a 412dp phone, a tablet upright next to the rail (800 − 80) and sideways (1280 − 80).
 */
class AdaptiveLayoutTest {

    private val phone = 412.dp - 32.dp
    private val tabletUpright = 800.dp - 80.dp - 32.dp
    private val tabletSideways = 1280.dp - 80.dp - 32.dp

    @Test
    fun `the library grid keeps two covers on a phone and fits more on a tablet`() {
        fun grid(width: Dp) = columnCount(width, minCellWidth = 160.dp, spacing = 16.dp, minColumns = 2)
        assertEquals(2, grid(phone))
        assertEquals(2, grid(360.dp - 32.dp))
        assertEquals(4, grid(tabletUpright))
        assertEquals(6, grid(tabletSideways))
    }

    @Test
    fun `lists and series cards are one across on a phone and two or three on a tablet`() {
        fun cards(width: Dp) = columnCount(width, minCellWidth = 320.dp, spacing = 16.dp)
        assertEquals(1, cards(phone))
        assertEquals(2, cards(tabletUpright))
        assertEquals(3, cards(tabletSideways))
    }

    @Test
    fun `the compact series grid keeps three on a phone`() {
        fun compact(width: Dp) = columnCount(width, minCellWidth = 110.dp, spacing = 12.dp, minColumns = 3)
        assertEquals(3, compact(phone))
        assertEquals(3, compact(300.dp))
        assertEquals(5, compact(tabletUpright))
    }

    @Test
    fun `a window too narrow for one cell still gets the minimum`() {
        assertEquals(1, columnCount(100.dp, minCellWidth = 320.dp, spacing = 16.dp))
        assertEquals(2, columnCount(100.dp, minCellWidth = 160.dp, spacing = 16.dp, minColumns = 2))
    }

    @Test
    fun `grid cells share out the whole width`() {
        val density = Density(1f)
        fun sizes(cells: GridCells, available: Int, spacing: Int) =
            with(cells) { density.calculateCrossAxisCellSizes(available, spacing) }

        // 808dp is Series Details at its widest: four covers.
        assertEquals(List(4) { 190 }, sizes(AtLeastCells(160.dp, minColumns = 2), available = 808, spacing = 16))

        // Pixels left over go to the first cells, so the row is filled exactly.
        val uneven = sizes(AtLeastCells(160.dp, minColumns = 2), available = 810, spacing = 16)
        assertEquals(listOf(191, 191, 190, 190), uneven)
        assertEquals(810 - 3 * 16, uneven.sum())

        val phoneCells = sizes(AtLeastCells(160.dp, minColumns = 2), available = 380, spacing = 16)
        assertEquals(listOf(182, 182), phoneCells)
    }
}
