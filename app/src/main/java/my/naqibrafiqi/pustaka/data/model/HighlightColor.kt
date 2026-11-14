package my.naqibrafiqi.pustaka.data.model

import androidx.annotation.ColorInt

/**
 * The colours a highlight can have (decision D13). The reader draws them see-through over the
 * text, so they're stored fully opaque.
 *
 * @property argb what's saved in the database.
 */
enum class HighlightColor(val label: String, @param:ColorInt val argb: Int) {
    YELLOW("Yellow", 0xFFFFD54F.toInt()),
    GREEN("Green", 0xFF81C784.toInt()),
    BLUE("Blue", 0xFF64B5F6.toInt()),
    PINK("Pink", 0xFFF06292.toInt());

    companion object {
        /** Used for the very first highlight. */
        val DEFAULT = YELLOW

        /** A colour saved by another version of the app that this one doesn't offer becomes [DEFAULT]. */
        fun of(argb: Int): HighlightColor = entries.firstOrNull { it.argb == argb } ?: DEFAULT
    }
}
