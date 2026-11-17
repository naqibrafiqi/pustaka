package my.naqibrafiqi.pustaka.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Windows at least this wide (tablets, unfolded foldables, phones on their side) get a navigation
 * rail down the side instead of the bottom bar. It's Material's lower bound for a medium window.
 */
val WideWindowWidth = 600.dp

/** The widest that lists, settings and details get, so their lines don't stretch across a tablet. */
val MaxContentWidth = 720.dp

/**
 * How many columns at least [minCellWidth] wide fit across [width], with [spacing] between them.
 * Never fewer than [minColumns], so a narrow phone keeps the layout it was designed with.
 */
fun columnCount(width: Dp, minCellWidth: Dp, spacing: Dp, minColumns: Int = 1): Int {
    val fit = ((width + spacing) / (minCellWidth + spacing)).toInt()
    return maxOf(minColumns, fit)
}

/** Grid cells for a lazy grid that follow [columnCount]: as many as fit, but at least [minColumns]. */
class AtLeastCells(private val minCellWidth: Dp, private val minColumns: Int = 1) : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val count = columnCount(availableSize.toDp(), minCellWidth, spacing.toDp(), minColumns)
        val cellsWidth = availableSize - spacing * (count - 1)
        val cell = cellsWidth / count
        val extra = cellsWidth % count
        return List(count) { index -> cell + if (index < extra) 1 else 0 }
    }

    override fun equals(other: Any?): Boolean =
        other is AtLeastCells && other.minCellWidth == minCellWidth && other.minColumns == minColumns

    override fun hashCode(): Int = 31 * minCellWidth.hashCode() + minColumns
}

/**
 * Centres the content and keeps it to [maxWidth]. Narrower windows, like phones, are unchanged.
 * The space either side stays empty, so put this on the scrolling list, not on the screen's top bar.
 */
fun Modifier.centredContentWidth(maxWidth: Dp = MaxContentWidth): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = maxWidth)
    .fillMaxWidth()

/** Like `fillMaxWidth(fraction)`, but never wider than [max], so a cover doesn't grow with a tablet. */
fun Modifier.fillMaxWidthUpTo(fraction: Float, max: Dp): Modifier = layout { measurable, constraints ->
    val wanted = if (constraints.hasBoundedWidth) (constraints.maxWidth * fraction).roundToInt() else Constraints.Infinity
    val width = minOf(wanted, max.roundToPx()).coerceIn(constraints.minWidth, constraints.maxWidth)
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}
