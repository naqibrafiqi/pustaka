package my.naqibrafiqi.pustaka.ui.model

import my.naqibrafiqi.pustaka.data.model.ReadingSession
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The "Velocity" and "Estimated Left" tiles in Book Details, worked out from the book's reading
 * sessions, which the reader records (see `SessionTracker`). Both show "—" until there's at least a
 * minute of reading with pages turned.
 *
 * Pages here are Readium positions, the same pages as in "Page X of Y".
 */
class ReadingStats(sessions: List<ReadingSession>) {

    private val pagesRead: Int = sessions.sumOf { it.pagesRead }
    private val timeRead: Duration = sessions.fold(Duration.ZERO) { total, session -> total + session.duration }

    /** Pages per hour, or null when there isn't at least a minute of reading with pages turned. */
    val pagesPerHour: Double?
        get() = if (pagesRead > 0 && timeRead >= MIN_READING_TIME) {
            pagesRead * 3600.0 / timeRead.seconds
        } else {
            null
        }

    /** The time left at the usual speed, or null when there's no speed to go on. */
    fun timeLeft(pagesLeft: Int): Duration? {
        val speed = pagesPerHour ?: return null
        if (pagesLeft <= 0) return Duration.ZERO
        return Duration.ofMinutes(ceil(pagesLeft / speed * 60).toLong())
    }

    companion object {
        private val MIN_READING_TIME: Duration = Duration.ofMinutes(1)
        const val NONE = "—"
    }
}

/** "Avg. 42 pages/h", or "—". */
fun velocityLabel(pagesPerHour: Double?): String =
    pagesPerHour?.let { "Avg. ${it.roundToInt()} pages/h" } ?: ReadingStats.NONE

/** "~2h 15m", "~45m", "<1m", or "—". */
fun timeLeftLabel(timeLeft: Duration?): String {
    if (timeLeft == null) return ReadingStats.NONE
    val minutes = timeLeft.toMinutes()
    return when {
        minutes < 1 -> "<1m"
        minutes < 60 -> "~${minutes}m"
        minutes % 60 == 0L -> "~${minutes / 60}h"
        else -> "~${minutes / 60}h ${minutes % 60}m"
    }
}

/** "Read just now", "Read 32m ago", "Read 3h ago", "Read 2d ago", or "Read on 4 Oct 2026". */
fun readAgoLabel(openedAt: Instant?, now: Instant, zone: ZoneId = ZoneId.systemDefault()): String? {
    if (openedAt == null) return null
    val elapsed = Duration.between(openedAt, now).coerceAtLeast(Duration.ZERO)
    return when {
        elapsed < Duration.ofMinutes(1) -> "Read just now"
        elapsed < Duration.ofHours(1) -> "Read ${elapsed.toMinutes()}m ago"
        elapsed < Duration.ofDays(1) -> "Read ${elapsed.toHours()}h ago"
        elapsed < Duration.ofDays(30) -> "Read ${elapsed.toDays()}d ago"
        else -> "Read on ${DATE.format(openedAt.atZone(zone))}"
    }
}

/** "14.2 MB", "820 KB" or "0 B". */
fun fileSizeLabel(bytes: Long): String = when {
    bytes >= MB -> String.format(Locale.US, "%.1f MB", bytes / MB.toDouble())
    bytes >= KB -> "${bytes / KB} KB"
    else -> "$bytes B"
}

private const val KB = 1024L
private const val MB = KB * 1024

private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

private fun Duration.coerceAtLeast(minimum: Duration): Duration = if (this < minimum) minimum else this
