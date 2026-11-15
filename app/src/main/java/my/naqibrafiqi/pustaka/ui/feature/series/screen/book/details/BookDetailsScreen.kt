package my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.BookCover
import my.naqibrafiqi.pustaka.ui.components.ErrorState
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.components.SynopsisSection
import my.naqibrafiqi.pustaka.ui.components.TagChip
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.components.menu.DropdownMenuButton
import my.naqibrafiqi.pustaka.ui.components.menu.MenuItem
import my.naqibrafiqi.pustaka.ui.model.BookState
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asString
import my.naqibrafiqi.pustaka.util.text.asUiText

@Composable
fun BookDetailsScreen(
    state: BookDetailsUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRead: (fromStart: Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    BookDetailsScreenContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onRetry = onRetry,
        onEdit = onEdit,
        onDelete = onDelete,
        onRead = onRead,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun BookDetailsScreenContent(
    state: BookDetailsUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRead: (fromStart: Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val ready = state as? BookDetailsUiState.Ready

    var openMenu by remember { mutableStateOf(false) }

    var showInfo by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBar(
                title = stringResource(R.string.book_details_title),
                subtitle = ready?.let { it.seriesLine?.asString() ?: it.title },
                onNavigateBack = onNavigateBack,
                actionButtons = if (ready == null) null else {
                    {
                        DropdownMenuButton(
                            expanded = openMenu,
                            openMenu = {
                                openMenu = true
                            },
                            closeMenu = {
                                openMenu = false
                            }
                        ) {
                            MenuItem(
                                onClick = {
                                    openMenu = false
                                    showInfo = true
                                },
                                text = stringResource(R.string.book_details_more_info),
                                leadingIconResId = R.drawable.info
                            )

                            MenuItem(
                                onClick = {
                                    openMenu = false
                                    onEdit()
                                },
                                text = stringResource(R.string.details_edit),
                                leadingIconResId = R.drawable.edit
                            )

                            MenuItem(
                                onClick = {
                                    openMenu = false
                                    onDelete()
                                },
                                text = stringResource(R.string.book_delete),
                                leadingIconResId = R.drawable.delete
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())

        when (state) {
            BookDetailsUiState.Loading -> LoadingState(modifier = contentModifier)
            is BookDetailsUiState.Error -> ErrorState(
                message = stringResource(state.message),
                onRetry = onRetry,
                modifier = contentModifier,
            )
            BookDetailsUiState.NotFound -> MessageState(
                title = stringResource(R.string.book_details_not_found_title),
                message = stringResource(R.string.book_details_not_found_message),
                iconRes = R.drawable.book_5,
                modifier = contentModifier,
                actionLabel = stringResource(R.string.common_go_back),
                onAction = onNavigateBack,
            )
            is BookDetailsUiState.Ready -> ReadyContent(
                state = state,
                onRead = onRead,
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp),
                modifier = contentModifier,
            )
        }
    }

    if (showInfo && ready != null) {
        MoreInfoDialog(rows = ready.info, onDismiss = { showInfo = false })
    }
}

@Composable
private fun ReadyContent(
    state: BookDetailsUiState.Ready,
    onRead: (fromStart: Boolean) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BookCover(
                    modifier = Modifier.fillMaxWidth(.5f),
                    imageUrl = state.coverPath
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = state.title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )

                state.credits?.let { credits ->
                    Text(
                        text = credits,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        if (state.genres.isNotEmpty() || !state.synopsis.isNullOrBlank()) {
            item {
                SynopsisSection(genres = state.genres, synopsis = state.synopsis)
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProgressSection(state)

                ReadButtons(state, onRead)
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BasicStatisticsTile(
                        labelText = stringResource(R.string.book_details_page_count),
                        labelIconRes = R.drawable.article,
                        valueText = state.pageCount.asString(),
                        modifier = Modifier.weight(1f),
                    )

                    BasicStatisticsTile(
                        labelText = stringResource(R.string.book_details_velocity),
                        labelIconRes = R.drawable.speed,
                        valueText = state.velocity.asString(),
                        modifier = Modifier.weight(1f),
                    )
                }

                BasicStatisticsTile(
                    labelText = stringResource(R.string.book_details_time_left),
                    labelIconRes = R.drawable.schedule,
                    valueText = state.timeLeft.asString(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ProgressSection(state: BookDetailsUiState.Ready) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.common_percent, state.percent),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleLarge
                )

                TagChip(
                    text = stringResource(statusChipLabel(state.state)),
                    color = statusChipColor(state.state)
                )
            }

            state.pageLabel?.let { label ->
                Text(
                    text = label.asString(),
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        LinearProgressIndicator(
            progress = { state.percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            gapSize = 0.dp
        )
    }
}

/**
 * "Start Reading" or "Continue From Page N", plus "Read From Beginning" once the book has been
 * started. A finished book is read again from its first page.
 */
@Composable
private fun ReadButtons(state: BookDetailsUiState.Ready, onRead: (fromStart: Boolean) -> Unit) {
    val finished = state.state == BookState.FINISHED

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onRead(finished) },
            enabled = state.canRead,
            modifier = Modifier.fillMaxWidth()
        ) {
            ButtonContent(
                iconRes = R.drawable.book_5,
                text = when {
                    finished -> stringResource(R.string.book_read_again)
                    state.continuePage != null -> stringResource(R.string.book_continue_from_page, state.continuePage)
                    else -> stringResource(R.string.book_start_reading)
                }
            )
        }

        if (state.continuePage != null) {
            OutlinedButton(
                onClick = { onRead(true) },
                enabled = state.canRead,
                modifier = Modifier.fillMaxWidth()
            ) {
                ButtonContent(iconRes = R.drawable.menu_book, text = stringResource(R.string.book_read_from_beginning))
            }
        }
    }
}

@Composable
private fun ButtonContent(@DrawableRes iconRes: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(text = text)
    }
}

@StringRes
private fun statusChipLabel(state: BookState): Int = when (state) {
    BookState.UNREAD -> R.string.book_status_not_started
    BookState.READING -> R.string.book_status_in_progress
    BookState.FINISHED -> R.string.book_state_finished
    BookState.MISSING -> R.string.book_state_missing
}

@Composable
private fun statusChipColor(state: BookState): Color = when (state) {
    BookState.MISSING -> MaterialTheme.colorScheme.errorContainer
    BookState.FINISHED -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.tertiary
}

@Composable
private fun MoreInfoDialog(rows: List<InfoRow>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        },
        title = { Text(stringResource(R.string.book_details_more_info)) },
        text = {
            SelectionContainer {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rows.forEach { row ->
                        Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                            Text(
                                text = stringResource(row.label),
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(text = row.value.asString(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun BasicStatisticsTile(
    labelText: String,
    @DrawableRes labelIconRes: Int,
    valueText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = labelIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )

                Text(
                    text = labelText,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = valueText,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookDetailsScreenPreview() {
    PustakaTheme {
        BookDetailsScreenContent(
            state = BookDetailsUiState.Ready(
                title = "Playing Death Games, Vol. 01",
                seriesLine = "Playing Death Games · Volume 1".asUiText(),
                credits = "Yushi Ukai, Nekometaru",
                coverPath = null,
                genres = listOf("Drama", "Fantasy", "Action"),
                synopsis = "Yuki wakes up to find herself in an unfamiliar manor.",
                state = BookState.READING,
                percent = 58,
                pageLabel = "Page 116 of 200".asUiText(),
                continuePage = 116,
                pageCount = "200 pages".asUiText(),
                velocity = "Avg. 42 pages/h".asUiText(),
                timeLeft = "~2h 15m".asUiText(),
                info = emptyList(),
            ),
            onNavigateBack = {},
            onRetry = {},
            onEdit = {},
            onDelete = {},
            onRead = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
