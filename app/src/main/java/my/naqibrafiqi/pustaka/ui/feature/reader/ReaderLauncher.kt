package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Opens a book in the reader. */
interface ReaderLauncher {
    /** @param fromStart opens at the first page, forgetting the saved position. */
    fun open(bookId: Long, fromStart: Boolean)

    /** Opens the book at [locator], a serialized Readium `Locator` such as a highlight's. */
    fun openAt(bookId: Long, locator: String)
}

@Composable
fun rememberReaderLauncher(): ReaderLauncher {
    val context = LocalContext.current
    return remember(context) {
        object : ReaderLauncher {
            override fun open(bookId: Long, fromStart: Boolean) {
                context.startActivity(ReaderActivity.intent(context, bookId, fromStart))
            }

            override fun openAt(bookId: Long, locator: String) {
                context.startActivity(ReaderActivity.intent(context, bookId, locator = locator))
            }
        }
    }
}
