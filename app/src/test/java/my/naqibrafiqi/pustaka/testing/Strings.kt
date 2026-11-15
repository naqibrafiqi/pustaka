package my.naqibrafiqi.pustaka.testing

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import my.naqibrafiqi.pustaka.util.text.UiText
import my.naqibrafiqi.pustaka.util.text.resolve

/** The app's resources. Only works in tests that run with Robolectric. */
val testResources: Resources get() = ApplicationProvider.getApplicationContext<Context>().resources

/** The English text the user would see. */
fun UiText.text(): String = resolve(testResources)

fun string(@StringRes id: Int, vararg args: Any): String = testResources.getString(id, *args)
