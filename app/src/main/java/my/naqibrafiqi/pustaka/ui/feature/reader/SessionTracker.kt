package my.naqibrafiqi.pustaka.ui.feature.reader

import java.time.Duration
import java.time.Instant

/** A stretch of reading to record, which feeds "Velocity" and "Estimated Left" in Book Details. */
data class TrackedSession(val startedAt: Instant, val endedAt: Instant, val pagesRead: Int)

/**
 * Turns page changes into reading sessions.
 *
 * A session starts on the first page shown and ends when the reader goes into the background. If
 * no page is turned for [idleLimit], the reader is taken to have put the book down: the session
 * ends at the last page turn, and the next page turn starts a new one. That keeps a book left open
 * on the table from counting as hours of reading.
 *
 * Pages read are the different pages shown after the first one, so going back and forth over the
 * same pages doesn't count them twice. Sessions without any pages read aren't recorded.
 */
class SessionTracker(private val idleLimit: Duration = DEFAULT_IDLE_LIMIT) {

    private var startedAt: Instant? = null
    private var lastActivity: Instant = Instant.EPOCH
    private val pages = mutableSetOf<Int>()

    val isActive: Boolean get() = startedAt != null

    /**
     * Called for every page shown, including the first one.
     *
     * @return a session that ended because the reader had stopped for a while, to be recorded.
     */
    fun onPage(page: Int, now: Instant): TrackedSession? {
        val ended = if (isActive && isIdle(now)) close() else null
        if (startedAt == null) startedAt = now
        pages += page
        lastActivity = now
        return ended
    }

    /**
     * The reader went into the background or closed.
     *
     * @return the session to record, or null when there's nothing worth recording.
     */
    fun stop(now: Instant): TrackedSession? {
        if (!isActive) return null
        if (!isIdle(now)) lastActivity = now
        return close()
    }

    private fun isIdle(now: Instant): Boolean = Duration.between(lastActivity, now) > idleLimit

    private fun close(): TrackedSession? {
        val start = checkNotNull(startedAt)
        val session = TrackedSession(start, lastActivity, pages.size - 1).takeIf { it.pagesRead > 0 }
        startedAt = null
        pages.clear()
        return session
    }

    companion object {
        val DEFAULT_IDLE_LIMIT: Duration = Duration.ofMinutes(5)
    }
}
