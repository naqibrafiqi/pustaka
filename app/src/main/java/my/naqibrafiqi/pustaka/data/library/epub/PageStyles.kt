package my.naqibrafiqi.pustaka.data.library.epub

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingContainer
import org.readium.r2.shared.util.resource.map

/**
 * Pustaka's own touches to how a book's pages look, added to each chapter as the reader opens it.
 * Nothing is written back to the file.
 *
 * - Headings are centred, without the first-line indent many books give them (#34).
 * - With Justify on (decision D24), paragraphs the book leaves aligned to the left are justified.
 *   Paragraphs the book centres or aligns to the right keep their alignment (#32).
 *
 * A script does this instead of a stylesheet: Readium leaves out its default styles for a chapter
 * that has any styles of its own, and only a script can tell which paragraphs the book aligns.
 */
object PageStyles {

    /**
     * What turns justifying on. Readium only writes its ligatures property into the page, where
     * the script's styles look for it, without changing anything for books in left-to-right
     * languages. Unlike a style added when the chapter opens, Readium updates it on the open page
     * as soon as the setting changes.
     */
    const val JUSTIFY_SIGNAL = true

    private const val JUSTIFIED = "data-pustaka-justify"

    // No "<" or "&" in it, so it stays valid XHTML, and no "<style", which would count as the
    // chapter's own styles to Readium.
    private val SCRIPT = """
        <script type="text/javascript">
        (function () {
            var style = document.createElement('style');
            style.textContent =
                ':root :is(h1, h2, h3, h4, h5, h6) { text-align: center; text-indent: 0; }' +
                ':root[style*="--USER__ligatures"] [$JUSTIFIED] { text-align: justify !important; }';
            document.head.appendChild(style);
            document.addEventListener('DOMContentLoaded', function () {
                var blocks = document.querySelectorAll('p, div, li');
                for (var i = 0; i !== blocks.length; i++) {
                    var align = getComputedStyle(blocks[i]).textAlign;
                    if (align === 'start' || align === 'left') blocks[i].setAttribute('$JUSTIFIED', '');
                }
            });
        })();
        </script>
    """.trimIndent() + "\n"

    /** Adds the page script to every HTML chapter of the publication. */
    val onCreatePublication: Publication.Builder.() -> Unit = {
        container = TransformingContainer(container) { url: Url, resource: Resource ->
            if (url.isHtml()) resource.map { Try.success(inject(it)) } else resource
        }
    }

    /**
     * [html] with the page script at the end of its head. The bytes are searched as they are, so
     * any encoding that keeps ASCII as it is works. A chapter without a head is left alone.
     */
    fun inject(html: ByteArray): ByteArray {
        // ISO-8859-1 maps each byte to one character, so indexes match the bytes.
        val index = String(html, Charsets.ISO_8859_1).indexOf("</head", ignoreCase = true)
        if (index < 0) return html
        return html.copyOfRange(0, index) + SCRIPT.toByteArray(Charsets.UTF_8) + html.copyOfRange(index, html.size)
    }

    private fun Url.isHtml(): Boolean =
        toString().substringBefore('?').substringBefore('#').substringAfterLast('.').lowercase() in HTML_EXTENSIONS

    private val HTML_EXTENSIONS = setOf("xhtml", "html", "htm")
}
