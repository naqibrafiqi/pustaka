package my.naqibrafiqi.pustaka.ui.feature.reader

/**
 * Where reading was before a jump, offered as "Back to page 42" (#35).
 *
 * @property target the place to go back to, as a Readium `Locator` in the reader.
 * @property page its page, starting at 1, or null when the book has no page numbers.
 */
data class ReturnPoint<T>(val target: T, val page: Int?)

/**
 * Remembers the page before a jump from the table of contents, the progress bar or a highlight,
 * so the reader can go back to it (decision D26).
 *
 * The page is forgotten once [PAGES_TO_FORGET] pages have been read forward from where the jump
 * landed, on getting back to the remembered page, or when it's used. Closing the book forgets it
 * too, because it isn't saved.
 *
 * Jumping again before then keeps the first page, the one that was being read.
 */
class ReturnTracker<T> {

    var point: ReturnPoint<T>? = null
        private set

    /** The page the jump landed on, which reading forward is counted from. */
    private var landingPage: Int? = null

    /**
     * Called just before jumping.
     *
     * @param fromPage the page being left, or null when it isn't known.
     * @param toPage the page being jumped to, or null to take the next page shown.
     */
    fun onJump(from: T, fromPage: Int?, toPage: Int?) {
        if (point == null) point = ReturnPoint(from, fromPage)
        landingPage = toPage
        if (toPage != null && toPage == point?.page) forget()
    }

    /**
     * Called each time the page changes.
     *
     * @return true when the remembered page was forgotten.
     */
    fun onPage(page: Int): Boolean {
        val current = point ?: return false
        val landing = landingPage ?: page.also { landingPage = it }
        if (page == current.page || page - landing >= PAGES_TO_FORGET) {
            forget()
            return true
        }
        return false
    }

    /** The place to go back to, which is then forgotten. */
    fun take(): T? = point?.target.also { forget() }

    private fun forget() {
        point = null
        landingPage = null
    }

    companion object {
        /** How many pages forward from the jump count as reading on from there (D26). */
        const val PAGES_TO_FORGET = 10
    }
}
