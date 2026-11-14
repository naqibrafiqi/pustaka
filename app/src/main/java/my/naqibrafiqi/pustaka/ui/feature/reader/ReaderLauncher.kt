package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Opens a book in the reader. */
fun interface ReaderLauncher {
    /** @param fromStart opens at the first page, forgetting the saved position. */
    fun open(bookId: Long, fromStart: Boolean)
}

@Composable
fun rememberReaderLauncher(): ReaderLauncher {
    val context = LocalContext.current
    return remember(context) {
        ReaderLauncher { bookId, fromStart ->
            context.startActivity(ReaderActivity.intent(context, bookId, fromStart))
        }
    }
}
