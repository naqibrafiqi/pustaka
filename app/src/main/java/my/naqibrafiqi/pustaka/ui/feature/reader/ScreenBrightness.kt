package my.naqibrafiqi.pustaka.ui.feature.reader

import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * The reader's own screen brightness (#54, D31). Android can't pause auto-brightness for one app,
 * but a window can set its own brightness, which replaces the phone's while it's showing. The
 * phone's brightness and auto-brightness come back as soon as the window is left.
 *
 * Brightness is kept as a slider position, which follows the same curve as the phone's own
 * brightness slider, so the middle of the slider looks like the middle of the phone's.
 */
object ScreenBrightness {

    /** The phone's brightness right now, as a slider position, or null when it can't be read. */
    fun phoneLevel(context: Context): Float? = try {
        val setting = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        toLevel(setting / MAX_SETTING)
    } catch (_: Settings.SettingNotFoundException) {
        null
    }

    /** The window brightness, from 0 to 1, for a slider position. */
    fun toWindowBrightness(level: Float): Float {
        val position = level.coerceIn(0f, 1f)
        val hlg = if (position <= R) (position / R).let { it * it } else exp((position - C) / A) + B
        // A brightness of exactly 0 can turn the screen off on some phones.
        return (hlg / HLG_MAX).coerceIn(MIN_WINDOW_BRIGHTNESS, 1f)
    }

    /** The slider position for a window brightness from 0 to 1. The reverse of [toWindowBrightness]. */
    fun toLevel(windowBrightness: Float): Float {
        val hlg = windowBrightness.coerceIn(0f, 1f) * HLG_MAX
        val position = if (hlg <= 1f) sqrt(hlg) * R else A * ln(hlg - B) + C
        return position.coerceIn(0f, 1f)
    }

    /** Sets [activity]'s brightness to [level], or back to the phone's for null. */
    fun apply(activity: Activity, level: Float?) {
        val window = activity.window
        window.attributes = window.attributes.apply {
            screenBrightness = level?.let(::toWindowBrightness) ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    // The hybrid log-gamma curve Android's brightness slider uses.
    private const val R = 0.5f
    private const val A = 0.17883277f
    private const val B = 0.28466892f
    private const val C = 0.55991073f
    private const val HLG_MAX = 12f

    private const val MAX_SETTING = 255f
    private const val MIN_WINDOW_BRIGHTNESS = 0.001f
}

/**
 * Shows the screen at [level] while this is on screen, or at the phone's brightness for null.
 * The phone's brightness comes back when it leaves.
 */
@Composable
fun ScreenBrightnessEffect(level: Float?) {
    val activity = LocalActivity.current ?: return
    LaunchedEffect(activity, level) { ScreenBrightness.apply(activity, level) }
    DisposableEffect(activity) {
        onDispose { ScreenBrightness.apply(activity, null) }
    }
}
