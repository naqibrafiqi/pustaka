package my.naqibrafiqi.pustaka.ui.feature.reader

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IllustrationsTest {

    private val chapter = Url("epub/text/chapter-1.xhtml")!!

    @Test
    fun aPictureIsFoundFromTheChapterThatShowsIt() {
        assertEquals("epub/images/illustration-2.png", pictureHref(chapter, "../images/illustration-2.png").toString())
        assertEquals("epub/text/figure.jpg", pictureHref(chapter, "figure.jpg").toString())
        assertEquals("epub/images/a.png", pictureHref(chapter, " ../images/a.png#part ").toString())
    }

    @Test
    fun addressesWithSpacesWorkWhetherOrNotTheBookEncodesThem() {
        val encoded = pictureHref(chapter, "../images/my%20picture.png")
        val plain = pictureHref(chapter, "../images/my picture.png")

        assertEquals("epub/images/my picture.png", encoded?.path)
        assertEquals(encoded, plain)
    }

    @Test
    fun picturesFromOutsideTheBookAreNotOpened() {
        assertNull(pictureHref(chapter, "https://example.com/picture.png"))
        assertNull(pictureHref(chapter, "data:image/png;base64,AAAA"))
        assertNull(pictureHref(chapter, "  "))
    }

    @Test
    fun theBridgePassesOnThePictureAndItsDescription() {
        val opened = mutableListOf<Pair<Url, String?>>()
        val bridge = IllustrationBridge(chapter) { href, description -> opened += href to description }

        bridge.open("../images/rabbit.png", "  A white rabbit. ")
        bridge.open("../images/blank.png", "")
        bridge.open("https://example.com/x.png", "Outside")

        assertEquals(
            listOf("epub/images/rabbit.png" to "A white rabbit.", "epub/images/blank.png" to null),
            opened.map { it.first.toString() to it.second },
        )
    }

    @Test
    fun picturesAreDecodedAtFullSize() {
        val bitmap = decodeIllustration(png(width = 300, height = 200))

        assertEquals(300, bitmap?.width)
        assertEquals(200, bitmap?.height)
    }

    @Test
    fun hugePicturesAreDecodedSmallerToFitInMemory() {
        val bitmap = decodeIllustration(png(width = 1000, height = 400), maxSide = 300)

        // Divided by 4, the smallest power of two that brings 1000 down to 300 or less.
        assertEquals(250, bitmap?.width)
        assertEquals(100, bitmap?.height)
    }

    @Test
    fun anythingThatIsntAPictureIsLeftAlone() {
        assertNull(decodeIllustration("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".toByteArray()))
        assertNull(decodeIllustration(ByteArray(0)))
    }

    @Test
    fun theSampleSizeIsThePowerOfTwoThatFits() {
        assertEquals(1, sampleSize(4096, 100, maxSide = 4096))
        assertEquals(2, sampleSize(4097, 100, maxSide = 4096))
        assertEquals(4, sampleSize(100, 12000, maxSide = 4096))
    }

    private fun png(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }
}
