package my.naqibrafiqi.pustaka.ui.feature.reader

import org.json.JSONArray
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * The text at the top of the screen, shown with a bookmark (#37).
 *
 * Readium's `firstVisibleElementLocator()` gives the whole first paragraph that shows, which often
 * starts on the screen before. So the page is asked for the text from its first visible line
 * instead, and Readium's paragraph is only used when that finds nothing.
 */
object PageText {

    /**
     * Finds the first line of text on screen, a few pixels at a time down the middle of the page,
     * goes back to where that line starts, and returns about [LENGTH] characters from there.
     *
     * The middle is used because the page's side margins belong to the columns of text next to it,
     * which are the screens before and after.
     */
    private val SCRIPT = """
        (function() {
          function lineTop(node, offset) {
            var range = document.createRange();
            var start = Math.min(offset, node.length - 1);
            if (start < 0) return null;
            range.setStart(node, start);
            range.setEnd(node, start + 1);
            var rects = range.getClientRects();
            return rects.length ? rects[0].top : null;
          }
          var x = window.innerWidth / 2;
          for (var y = 1; y < window.innerHeight; y += 8) {
            var range = document.caretRangeFromPoint(x, y);
            if (!range || range.startContainer.nodeType !== Node.TEXT_NODE) continue;
            var node = range.startContainer, offset = range.startOffset;
            var top = lineTop(node, offset);
            if (top === null || top < 0) continue;
            while (offset > 0) {
              var before = lineTop(node, offset - 1);
              if (before === null || Math.abs(before - top) > 2) break;
              offset--;
            }
            var text = node.textContent.substring(offset);
            var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
            walker.currentNode = node;
            while (text.trim().length < $LENGTH && walker.nextNode()) text += walker.currentNode.textContent;
            if (text.trim().length > 0) return text;
          }
          return null;
        })()
    """.trimIndent()

    @OptIn(ExperimentalReadiumApi::class)
    suspend fun of(navigator: EpubNavigatorFragment): String? {
        val firstLine = runCatching { navigator.evaluateJavascript(SCRIPT) }.getOrNull()?.let(::parseResult)
        return firstLine ?: runCatching { navigator.firstVisibleElementLocator()?.text?.highlight }.getOrNull()
    }

    /** The WebView gives back the script's result as JSON: a quoted string, or `null`. */
    fun parseResult(json: String): String? =
        runCatching { JSONArray("[$json]").optString(0, "") }.getOrNull()
            ?.takeUnless { json == "null" || it.isBlank() }

    private const val LENGTH = 200
}
