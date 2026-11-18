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
 * - Illustrations are centred (#50): pictures with no text beside them, on their own line. Small
 *   pictures in a line of text, and ones the book floats beside the text, stay where they are.
 * - A long press on a picture tells the app which one it is, through [IMAGES_INTERFACE], so it can
 *   be opened full screen (D35). A long press anywhere else still selects text.
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

    /**
     * The name of the app's object the script calls with `open(src, alt)` after a long press on a
     * picture: the picture's address as written in the chapter (the largest in its `srcset`), and
     * its description. The reader registers it with Readium for each chapter.
     */
    const val IMAGES_INTERFACE = "PustakaImages"

    private const val JUSTIFIED = "data-pustaka-justify"
    private const val ILLUSTRATION = "data-pustaka-illustration"
    private const val XLINK = "http://www.w3.org/1999/xlink"

    // No "<" or "&" in it, so it stays valid XHTML, and no "<style", which would count as the
    // chapter's own styles to Readium.
    private val SCRIPT = """
        <script type="text/javascript">
        (function () {
            var style = document.createElement('style');
            style.textContent =
                ':root :is(h1, h2, h3, h4, h5, h6) { text-align: center; text-indent: 0; }' +
                ':root[style*="--USER__ligatures"] [$JUSTIFIED] { text-align: justify !important; }' +
                ':root [$ILLUSTRATION] { display: block; margin-left: auto !important; margin-right: auto !important; }';
            document.head.appendChild(style);

            function isInline(element) {
                return getComputedStyle(element).display.indexOf('inline') === 0;
            }
            // The text and pictures on the same lines as the element's own content.
            function lineContent(element, found) {
                var nodes = element.childNodes;
                for (var i = 0; i !== nodes.length; i++) {
                    var node = nodes[i];
                    if (node.nodeType === Node.TEXT_NODE) {
                        found.text += node.textContent;
                    } else if (node.nodeType === Node.ELEMENT_NODE) {
                        if (node.localName === 'img' || node.localName === 'svg') found.pictures++;
                        else if (isInline(node)) lineContent(node, found);
                    }
                }
                return found;
            }
            // A picture with nothing else on its lines, and not floated beside the text.
            function isIllustration(picture) {
                if (getComputedStyle(picture).cssFloat !== 'none') return false;
                var block = picture.parentElement;
                while (block !== null) {
                    if (!isInline(block)) break;
                    block = block.parentElement;
                }
                if (block === null) return false;
                var found = lineContent(block, { text: '', pictures: 0 });
                return found.pictures === 1 ? found.text.trim() === '' : false;
            }

            document.addEventListener('DOMContentLoaded', function () {
                var blocks = document.querySelectorAll('p, div, li');
                for (var i = 0; i !== blocks.length; i++) {
                    var align = getComputedStyle(blocks[i]).textAlign;
                    if (align === 'start' || align === 'left') blocks[i].setAttribute('$JUSTIFIED', '');
                }
                var pictures = document.querySelectorAll('img, svg');
                for (var j = 0; j !== pictures.length; j++) {
                    if (isIllustration(pictures[j])) pictures[j].setAttribute('$ILLUSTRATION', '');
                }
            });

            // The largest picture in the srcset, or else the src.
            function largestSource(img) {
                var best = img.getAttribute('src');
                var bestSize = 0;
                var candidates = (img.getAttribute('srcset') || '').split(',');
                for (var i = 0; i !== candidates.length; i++) {
                    var parts = candidates[i].trim().split(/\s+/);
                    var size = parts.length === 1 ? 1 : parseFloat(parts[1]);
                    if (parts[0] !== '') {
                        if (size > bestSize) {
                            best = parts[0];
                            bestSize = size;
                        }
                    }
                }
                return best;
            }
            // The picture that was pressed, as { src, alt }, or null for anything else.
            function pictureAt(element) {
                for (; element !== null; element = element.parentElement) {
                    if (element.localName === 'img') {
                        return { src: largestSource(element), alt: element.getAttribute('alt') };
                    }
                    if (element.localName === 'svg') {
                        var image = element.querySelector('image');
                        if (image === null) return null;
                        var title = element.querySelector('title');
                        return {
                            src: image.getAttribute('href') || image.getAttributeNS('$XLINK', 'href'),
                            alt: element.getAttribute('aria-label') || (title === null ? null : title.textContent)
                        };
                    }
                }
                return null;
            }
            // Android sends a long press on anything that isn't text as a context menu event.
            document.addEventListener('contextmenu', function (event) {
                if (!window.$IMAGES_INTERFACE) return;
                var picture = pictureAt(event.target);
                if (picture === null) return;
                if (!picture.src) return;
                event.preventDefault();
                // Android may have started selecting the text next to the picture as well.
                window.getSelection().removeAllRanges();
                window.$IMAGES_INTERFACE.open(picture.src, picture.alt || '');
            }, true);
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
