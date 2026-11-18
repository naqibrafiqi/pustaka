package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomTest {

    private val screen = Size(1000f, 2000f)

    /** A landscape picture fitted to the width of a portrait screen. */
    private val picture = Size(1000f, 500f)

    @Test
    fun aPictureFitsTheScreenKeepingItsShape() {
        assertEquals(Size(1000f, 500f), fittedSize(Size(2000f, 1000f), screen))
        assertEquals(Size(500f, 2000f), fittedSize(Size(100f, 400f), screen))
        assertEquals(Size.Zero, fittedSize(Size(0f, 10f), screen))
    }

    @Test
    fun pinchingZoomsAroundTheFingers() {
        val zoom = Zoom().transformed(centroid = Offset(200f, 0f), pan = Offset.Zero, zoomBy = 2f, picture, screen)

        assertEquals(2f, zoom.scale)
        // The point that was 200 px right of the middle is still under the fingers.
        assertEquals(Offset(-200f, 0f), zoom.offset)
    }

    @Test
    fun zoomStaysBetweenTheWholePictureAndTheMost() {
        assertEquals(Zoom.MIN_SCALE, Zoom().transformed(Offset.Zero, Offset.Zero, 0.2f, picture, screen).scale)
        assertEquals(Zoom.MAX_SCALE, Zoom(5f).transformed(Offset.Zero, Offset.Zero, 3f, picture, screen).scale)
    }

    @Test
    fun theEdgesDontComeInsideTheScreen() {
        val zoomed = Zoom(scale = 2f)

        // 2000 px wide on a 1000 px screen: 500 px of room each way. 1000 px tall on a 2000 px screen: none.
        val dragged = zoomed.transformed(Offset.Zero, pan = Offset(900f, 300f), zoomBy = 1f, picture, screen)
        assertEquals(Offset(500f, 0f), dragged.offset)

        val unzoomed = Zoom().transformed(Offset.Zero, pan = Offset(100f, 100f), zoomBy = 1f, picture, screen)
        assertEquals(Offset.Zero, unzoomed.offset)
    }

    @Test
    fun doubleTapZoomsInToFillTheScreenThenBackOut() {
        // Filling the 2000 px height takes 4 times the 500 px picture.
        val zoomed = Zoom().toggled(tap = Offset(100f, 0f), picture, screen)
        assertEquals(4f, zoomed.scale)
        assertEquals(Offset(-300f, 0f), zoomed.offset)

        assertEquals(Zoom(), zoomed.toggled(Offset(100f, 0f), picture, screen))
    }

    @Test
    fun doubleTapZoomsInAtLeastTwice() {
        // A tall picture already nearly fills the screen.
        val tall = fittedSize(Size(900f, 2000f), screen)

        assertEquals(Zoom.DOUBLE_TAP_SCALE, Zoom().toggled(Offset.Zero, tall, screen).scale)
    }
}
