package my.naqibrafiqi.pustaka.ui.feature.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.JavascriptInterface
import my.naqibrafiqi.pustaka.data.library.epub.PageStyles
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import kotlin.math.max

/**
 * An illustration opened full screen from the page (#50, D35).
 *
 * @property description the book's description of it, for screen readers, when it has one.
 */
class Illustration(val bitmap: Bitmap, val description: String?)

/**
 * What the page script calls after a long press on a picture in [chapter] (see [PageStyles]).
 * Readium adds one to each chapter's page. It's called on a background thread.
 *
 * @param onOpen gets the picture's place in the book and its description.
 */
class IllustrationBridge(private val chapter: Url, private val onOpen: (Url, String?) -> Unit) {

    @JavascriptInterface
    fun open(src: String, alt: String) {
        val href = pictureHref(chapter, src) ?: return
        onOpen(href, alt.trim().ifEmpty { null })
    }

    companion object {
        const val NAME = PageStyles.IMAGES_INTERFACE
    }
}

/**
 * The place in the book of a picture that [chapter] shows as [src], as written in the chapter.
 * Null for a picture from outside the book, or an address that can't be read.
 */
fun pictureHref(chapter: Url, src: String): Url? {
    val written = src.trim().ifEmpty { return null }
    // Books don't always encode spaces and other characters in their addresses.
    val url = Url(written) ?: Url.fromDecodedPath(written) ?: return null
    if (url is AbsoluteUrl) return null
    return chapter.resolve(url).removeFragment()
}

/**
 * Decodes a picture from the book. Pictures much larger than any screen are decoded at a smaller
 * size, so they still fit in memory and can be drawn.
 *
 * @return null when the bytes aren't a picture Android can read, such as an SVG drawing.
 */
fun decodeIllustration(bytes: ByteArray, maxSide: Int = MAX_ILLUSTRATION_SIDE): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide) }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

/** The smallest power of two to divide the picture's sides by so the longer one is at most [maxSide]. */
fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
    var size = 1
    while (max(width, height) / size > maxSide) size *= 2
    return size
}

/** 4096 × 4096 is 64 MB, which can still be drawn on screen. */
const val MAX_ILLUSTRATION_SIDE = 4096
