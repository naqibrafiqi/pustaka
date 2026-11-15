package my.naqibrafiqi.pustaka.util.text

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import my.naqibrafiqi.pustaka.R

/**
 * Text that ViewModels and models hand to the UI without turning it into a string themselves.
 * The UI looks it up in the string resources when it's shown ([asString]), so all text lives in
 * `strings.xml` and a translation would follow the phone's language.
 *
 * Arguments can be [UiText] too, and are looked up first.
 */
sealed interface UiText {

    /** Text shown as it is: names, paths and text from a book. */
    data class Plain(val text: String) : UiText

    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** A plural string. With no [args], [count] is the only argument. */
    data class Plural(@param:PluralsRes val id: Int, val count: Int, val args: List<Any> = listOf(count)) : UiText

    /** [parts] joined with ", ". */
    data class Joined(val parts: List<UiText>, @param:StringRes val separator: Int = R.string.common_list_separator) : UiText
}

fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())

fun pluralText(@PluralsRes id: Int, count: Int, vararg args: Any): UiText =
    UiText.Plural(id, count, if (args.isEmpty()) listOf(count) else args.toList())

fun String.asUiText(): UiText = UiText.Plain(this)

fun UiText.resolve(resources: Resources): String = when (this) {
    is UiText.Plain -> text
    is UiText.Res -> if (args.isEmpty()) resources.getString(id) else resources.getString(id, *args.resolve(resources))
    is UiText.Plural -> resources.getQuantityString(id, count, *args.resolve(resources))
    is UiText.Joined -> parts.joinToString(resources.getString(separator)) { it.resolve(resources) }
}

private fun List<Any>.resolve(resources: Resources): Array<Any> =
    map { if (it is UiText) it.resolve(resources) else it }.toTypedArray()

/** The text, looked up again when the phone's language changes. */
@Composable
@ReadOnlyComposable
fun UiText.asString(): String = resolve(LocalResources.current)
