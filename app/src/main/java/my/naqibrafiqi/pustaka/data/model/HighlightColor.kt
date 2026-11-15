package my.naqibrafiqi.pustaka.data.model

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import my.naqibrafiqi.pustaka.R

/**
 * The colours a highlight can have (decision D13). The reader draws them see-through over the
 * text, so they're stored fully opaque.
 *
 * @property argb what's saved in the database.
 */
enum class HighlightColor(@param:StringRes val label: Int, @param:ColorInt val argb: Int) {
    YELLOW(R.string.highlight_color_yellow, 0xFFFFD54F.toInt()),
    GREEN(R.string.highlight_color_green, 0xFF81C784.toInt()),
    BLUE(R.string.highlight_color_blue, 0xFF64B5F6.toInt()),
    PINK(R.string.highlight_color_pink, 0xFFF06292.toInt());

    companion object {
        /** Used for the very first highlight. */
        val DEFAULT = YELLOW

        /** A colour saved by another version of the app that this one doesn't offer becomes [DEFAULT]. */
        fun of(argb: Int): HighlightColor = entries.firstOrNull { it.argb == argb } ?: DEFAULT
    }
}
