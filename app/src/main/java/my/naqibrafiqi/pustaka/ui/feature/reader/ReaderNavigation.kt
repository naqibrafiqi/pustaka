package my.naqibrafiqi.pustaka.ui.feature.reader

import org.readium.r2.shared.publication.Link

/** Where a tap on the page lands. The edges turn the page and the middle shows the menus. */
enum class TapZone {
    PREVIOUS,
    MENU,
    NEXT;

    companion object {
        /** The share of the page's width, on each side, that turns the page. */
        const val EDGE = 0.3f

        /**
         * @param x the tap's distance from the left of the page, in pixels.
         * @param rightToLeft for books read from right to left, where the left edge goes forward.
         */
        fun of(x: Float, width: Int, rightToLeft: Boolean = false): TapZone {
            if (width <= 0) return MENU
            val fraction = x / width
            val left = if (rightToLeft) NEXT else PREVIOUS
            val right = if (rightToLeft) PREVIOUS else NEXT
            return when {
                fraction < EDGE -> left
                fraction > 1 - EDGE -> right
                else -> MENU
            }
        }
    }
}

/**
 * One line of the table of contents.
 *
 * @property depth 0 for a top-level chapter, 1 for a section inside it, and so on.
 */
data class TocEntry(val title: String, val depth: Int, val link: Link) {
    /** The chapter's file inside the EPUB, without the `#section` part. */
    val resource: String get() = resourceOf(link.href.toString())
}

/** The table of contents as a flat list, in reading order. Entries without a title are skipped. */
fun flattenTableOfContents(links: List<Link>, depth: Int = 0): List<TocEntry> = links.flatMap { link ->
    val title = link.title?.trim()?.ifEmpty { null }
    val self = title?.let { listOf(TocEntry(it, depth, link)) }.orEmpty()
    // Children of an untitled entry move up a level, so they aren't indented under nothing.
    self + flattenTableOfContents(link.children, if (title != null) depth + 1 else depth)
}

/**
 * The entry for the chapter being read: the last one that points into [resource], so a section
 * wins over its chapter. When [resource] has no entry of its own, for example the pages between
 * two chapters, it's the entry before it in the book's [readingOrder].
 *
 * @return the index in [entries], or null when nothing matches.
 */
fun currentTocIndex(entries: List<TocEntry>, resource: String, readingOrder: List<String> = emptyList()): Int? {
    val current = resourceOf(resource)
    entries.indexOfLast { it.resource == current }.takeIf { it >= 0 }?.let { return it }

    val order = readingOrder.map(::resourceOf)
    val index = order.indexOf(current)
    if (index < 0) return null
    // Walk back through the book to the nearest file that the contents point to.
    for (earlier in order.subList(0, index).asReversed()) {
        entries.indexOfLast { it.resource == earlier }.takeIf { it >= 0 }?.let { return it }
    }
    return null
}

private fun resourceOf(href: String): String = href.substringBefore('#')
