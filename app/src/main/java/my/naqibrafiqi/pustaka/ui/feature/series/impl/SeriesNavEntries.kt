package my.naqibrafiqi.pustaka.ui.feature.series.impl

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.ui.feature.reader.rememberReaderLauncher
import my.naqibrafiqi.pustaka.ui.feature.search.api.SearchRoute
import my.naqibrafiqi.pustaka.ui.feature.series.api.BookDetailsRoute
import my.naqibrafiqi.pustaka.ui.feature.series.api.EditBookRoute
import my.naqibrafiqi.pustaka.ui.feature.series.api.EditSeriesRoute
import my.naqibrafiqi.pustaka.ui.feature.series.screen.BookDeleteDialogs
import my.naqibrafiqi.pustaka.ui.feature.series.screen.SeriesDeleteDialogs
import my.naqibrafiqi.pustaka.ui.feature.series.api.SeriesDetailsRoute
import my.naqibrafiqi.pustaka.ui.feature.series.api.SeriesRoute
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDetailsScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDetailsViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.edit.EditBookViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.edit.EditDetailsScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.edit.EditDetailsViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.edit.EditSeriesViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDetailsScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDetailsViewModel
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.SeriesScreen
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.main.SeriesViewModel
import my.naqibrafiqi.pustaka.ui.model.DetailsEvent
import my.naqibrafiqi.pustaka.ui.scan.LibraryScanViewModel
import my.naqibrafiqi.pustaka.util.provider.LocalAppNavigator

fun EntryProviderScope<NavKey>.seriesEntryBuilder(scanViewModel: LibraryScanViewModel) {
    entry<SeriesRoute> {
        val navigator = LocalAppNavigator.current
        val reader = rememberReaderLauncher()
        val viewModel: SeriesViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val isRefreshing by scanViewModel.isRefreshing.collectAsStateWithLifecycle()
        val createDialog by viewModel.createDialog.collectAsStateWithLifecycle()
        LaunchedEffect(viewModel) {
            viewModel.created.collect { seriesId -> navigator.add(SeriesDetailsRoute(seriesId)) }
        }

        SeriesScreen(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = scanViewModel::refresh,
            onRetry = viewModel::retry,
            onOpenSeries = { seriesId -> navigator.add(SeriesDetailsRoute(seriesId)) },
            onOpenBook = { bookId -> navigator.add(BookDetailsRoute(bookId)) },
            onReadBook = { bookId -> reader.open(bookId, fromStart = false) },
            createDialog = createDialog,
            onNewSeries = viewModel::openCreateDialog,
            onNewNameChange = viewModel::changeNewName,
            onCreateSeries = viewModel::createSeries,
            onDismissCreate = viewModel::dismissCreateDialog,
            onSearch = { navigator.add(SearchRoute) },
            onSelectSort = viewModel::selectSort,
            onToggleLayout = viewModel::toggleLayout,
        )
    }

    entry<SeriesDetailsRoute> { route ->
        val navigator = LocalAppNavigator.current
        val viewModel = hiltViewModel<SeriesDetailsViewModel, SeriesDetailsViewModel.Factory>(
            creationCallback = { factory -> factory.create(route.seriesId) }
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        val deleteDialog by viewModel.deleteDialog.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        DetailsEvents(viewModel.events, snackbarHostState, onClosed = { navigator.removeLastOrNull() })

        SeriesDetailsScreen(
            state = state,
            onNavigateBack = { navigator.removeLastOrNull() },
            onToggleLayout = viewModel::toggleLayout,
            onRetry = viewModel::retry,
            onOpenBook = { bookId -> navigator.add(BookDetailsRoute(bookId)) },
            onEdit = { navigator.add(EditSeriesRoute(route.seriesId)) },
            onDelete = viewModel::requestDelete,
            snackbarHostState = snackbarHostState,
        )

        SeriesDeleteDialogs(
            dialog = deleteDialog,
            onRemoveFromLibrary = viewModel::removeFromLibrary,
            onChooseDeleteFiles = viewModel::chooseDeleteFiles,
            onDeleteFolder = viewModel::deleteFolder,
            onDismiss = viewModel::dismissDeleteDialog,
        )
    }

    // Also opened from Library, so it's in that tab's back stack too.
    entry<BookDetailsRoute> { route ->
        val navigator = LocalAppNavigator.current
        val reader = rememberReaderLauncher()
        val viewModel = hiltViewModel<BookDetailsViewModel, BookDetailsViewModel.Factory>(
            creationCallback = { factory -> factory.create(route.bookId) }
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        val deleteDialog by viewModel.deleteDialog.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        DetailsEvents(viewModel.events, snackbarHostState, onClosed = { navigator.removeLastOrNull() })

        BookDetailsScreen(
            state = state,
            onNavigateBack = { navigator.removeLastOrNull() },
            onRetry = viewModel::retry,
            onEdit = { navigator.add(EditBookRoute(route.bookId)) },
            onDelete = viewModel::requestDelete,
            onRead = { fromStart -> reader.open(route.bookId, fromStart) },
            snackbarHostState = snackbarHostState,
        )

        BookDeleteDialogs(
            dialog = deleteDialog,
            onRemoveFromLibrary = viewModel::removeFromLibrary,
            onChooseDeleteFile = viewModel::chooseDeleteFile,
            onDeleteFile = viewModel::deleteFile,
            onForget = viewModel::forget,
            onDismiss = viewModel::dismissDeleteDialog,
        )
    }

    entry<EditSeriesRoute> { route ->
        val viewModel = hiltViewModel<EditSeriesViewModel, EditSeriesViewModel.Factory>(
            creationCallback = { factory -> factory.create(route.seriesId) }
        )
        EditEntry(title = "Edit series", viewModel = viewModel)
    }

    entry<EditBookRoute> { route ->
        val viewModel = hiltViewModel<EditBookViewModel, EditBookViewModel.Factory>(
            creationCallback = { factory -> factory.create(route.bookId) }
        )
        EditEntry(title = "Edit book", viewModel = viewModel)
    }
}

@Composable
private fun EditEntry(title: String, viewModel: EditDetailsViewModel) {
    val navigator = LocalAppNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.saved.collect { navigator.removeLastOrNull() }
    }

    EditDetailsScreen(
        title = title,
        state = state,
        onNavigateBack = { navigator.removeLastOrNull() },
        onChange = viewModel::change,
        onReset = viewModel::reset,
        onUseDetailsFromFile = viewModel::useDetailsFromFile,
        onSave = viewModel::save,
    )
}

/** Closes the screen once its series or book is gone, and shows messages in a snackbar. */
@Composable
private fun DetailsEvents(events: Flow<DetailsEvent>, snackbarHostState: SnackbarHostState, onClosed: () -> Unit) {
    val latestOnClosed by rememberUpdatedState(onClosed)
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                DetailsEvent.Closed -> latestOnClosed()
                is DetailsEvent.Message -> launch { snackbarHostState.showSnackbar(event.text) }
            }
        }
    }
}
