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
 * The page is forgotten once [TURNS_TO_FORGET] screens have been read forward from where the jump
 * landed, on getting back to the remembered page, or when it's used. Closing the book forgets it
 * too, because it isn't saved. Turning back a screen takes one off the count, so looking back
 * doesn't count as reading on.
 *
 * Jumping again before then keeps the first page, the one that was being read, and counts from
 * the new spot.
 */
class ReturnTracker<T> {

    var point: ReturnPoint<T>? = null
        private set

    /** Screens read forward from where the jump landed. */
    private var turns = 0

    /** The next move is the jump landing, which isn't a turn. */
    private var landing = false

    /**
     * Called just before jumping.
     *
     * @param fromPage the page being left, or null when it isn't known.
     */
    fun onJump(from: T, fromPage: Int?) {
        if (point == null) point = ReturnPoint(from, fromPage)
        turns = 0
        landing = true
    }

    /**
     * Called each time the place on screen changes.
     *
     * @param page the page now shown, or null when it isn't known.
     * @param turns screens moved: 1 for a turn forward, -1 for a turn back. When scrolling, the
     *   number of pages scrolled.
     * @return true when the remembered page was forgotten.
     */
    fun onMove(page: Int?, turns: Int): Boolean {
        val current = point ?: return false
        if (landing) {
            landing = false
        } else {
            this.turns += turns
        }
        if ((page != null && page == current.page) || this.turns >= TURNS_TO_FORGET) {
            forget()
            return true
        }
        return false
    }

    /** The place to go back to, which is then forgotten. */
    fun take(): T? = point?.target.also { forget() }

    private fun forget() {
        point = null
        turns = 0
        landing = false
    }

    companion object {
        /** How many screens forward from the jump count as reading on from there (D26). */
        const val TURNS_TO_FORGET = 10
    }
}
