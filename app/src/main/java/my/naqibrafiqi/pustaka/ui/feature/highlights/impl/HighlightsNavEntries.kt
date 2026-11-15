package my.naqibrafiqi.pustaka.ui.feature.highlights.impl

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.feature.highlights.api.HighlightsRoute
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.HighlightsEvent
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.HighlightsScreen
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.HighlightsUiState
import my.naqibrafiqi.pustaka.ui.feature.highlights.screen.HighlightsViewModel
import my.naqibrafiqi.pustaka.ui.feature.reader.HighlightSheet
import my.naqibrafiqi.pustaka.ui.feature.reader.rememberReaderLauncher

fun EntryProviderScope<NavKey>.highlightsEntryBuilder() {
    entry<HighlightsRoute> {
        val context = LocalContext.current
        val reader = rememberReaderLauncher()
        val viewModel: HighlightsViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val openHighlightId by viewModel.openHighlightId.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val resources = LocalResources.current

        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is HighlightsEvent.Share -> {
                        if (!shareFile(context, event)) snackbarHostState.showSnackbar(resources.getString(R.string.highlights_export_no_app))
                    }
                    is HighlightsEvent.Message -> snackbarHostState.showSnackbar(resources.getString(event.text))
                }
            }
        }

        HighlightsScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onSearch = viewModel::search,
            onRetry = viewModel::retry,
            onOpen = { highlight -> reader.openAt(highlight.bookId, highlight.locator) },
            onEdit = viewModel::openHighlight,
            onExportBook = viewModel::exportBook,
            onExportAll = viewModel::exportAll,
        )

        val openHighlight = (state as? HighlightsUiState.Ready)?.allGroups
            ?.asSequence()?.flatMap { it.books }?.flatMap { it.highlights }
            ?.firstOrNull { it.id == openHighlightId }?.highlight
        openHighlight?.let { highlight ->
            HighlightSheet(
                highlight = highlight,
                onChange = { color, note -> viewModel.updateHighlight(highlight.id, color, note) },
                onDelete = { viewModel.deleteHighlight(highlight.id) },
                onDismiss = viewModel::closeHighlight,
            )
        }
    }
}

/** Opens the share sheet with an exported Markdown file (decision D11). */
private fun shareFile(context: Context, share: HighlightsEvent.Share): Boolean {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/markdown")
        .putExtra(Intent.EXTRA_STREAM, share.uri)
        .putExtra(Intent.EXTRA_SUBJECT, share.fileName.removeSuffix(".md"))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // Lets the share sheet show the file's name and preview.
    send.clipData = ClipData.newRawUri(share.fileName, share.uri)
    return try {
        context.startActivity(Intent.createChooser(send, context.getString(R.string.highlights_export_chooser)))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
