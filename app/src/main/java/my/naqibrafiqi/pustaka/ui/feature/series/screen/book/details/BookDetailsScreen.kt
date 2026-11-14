package my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details

import androidx.annotation.DrawableRes
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

@Composable
fun BookDetailsScreen(
    state: BookDetailsUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    BookDetailsScreenContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onRetry = onRetry,
        onEdit = onEdit,
        onDelete = onDelete,
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
                title = "Book Details",
                subtitle = ready?.let { it.seriesLine ?: it.title },
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
                                text = "More info",
                                leadingIconResId = R.drawable.info
                            )

                            MenuItem(
                                onClick = {
                                    openMenu = false
                                    onEdit()
                                },
                                text = "Edit details",
                                leadingIconResId = R.drawable.edit
                            )

                            MenuItem(
                                onClick = {
                                    openMenu = false
                                    onDelete()
                                },
                                text = "Delete book",
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
                message = state.message,
                onRetry = onRetry,
                modifier = contentModifier,
            )
            BookDetailsUiState.NotFound -> MessageState(
                title = "Book not found",
                message = "This book is no longer in your library.",
                iconRes = R.drawable.book_5,
                modifier = contentModifier,
                actionLabel = "Go back",
                onAction = onNavigateBack,
            )
            is BookDetailsUiState.Ready -> ReadyContent(
                state = state,
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
                    style = MaterialTheme.typography.titleLarge
                )

                state.credits?.let { credits ->
                    Text(
                        text = credits,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = .8f),
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

                ReadButtons(state)
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
                        labelText = "Page Count",
                        labelIconRes = R.drawable.article,
                        valueText = state.pageCount,
                        modifier = Modifier.weight(1f),
                    )

                    BasicStatisticsTile(
                        labelText = "Velocity",
                        labelIconRes = R.drawable.speed,
                        valueText = state.velocity,
                        modifier = Modifier.weight(1f),
                    )
                }

                BasicStatisticsTile(
                    labelText = "Estimated Left",
                    labelIconRes = R.drawable.schedule,
                    valueText = state.timeLeft,
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
                    text = "${state.percent}%",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleLarge
                )

                TagChip(
                    text = statusChipLabel(state.state),
                    color = statusChipColor(state.state)
                )
            }

            state.pageLabel?.let { label ->
                Text(
                    text = label,
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

/** The reader arrives in Phase 4, so these buttons stay disabled until then. */
@Composable
private fun ReadButtons(state: BookDetailsUiState.Ready) {
    val readerAvailable = false

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { /* Opens the reader from Phase 4. */ },
            enabled = readerAvailable && state.canRead,
            modifier = Modifier.fillMaxWidth()
        ) {
            ButtonContent(
                iconRes = R.drawable.book_5,
                text = state.continuePage?.let { "Continue From Page $it" } ?: "Start Reading"
            )
        }

        if (state.continuePage != null) {
            OutlinedButton(
                onClick = { /* Opens the reader from Phase 4. */ },
                enabled = readerAvailable && state.canRead,
                modifier = Modifier.fillMaxWidth()
            ) {
                ButtonContent(iconRes = R.drawable.menu_book, text = "Read From Beginning")
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

private fun statusChipLabel(state: BookState): String = when (state) {
    BookState.UNREAD -> "Not Started"
    BookState.READING -> "In Progress"
    BookState.FINISHED -> "Finished"
    BookState.MISSING -> "File not found"
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
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("More info") },
        text = {
            SelectionContainer {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rows.forEach { row ->
                        Column {
                            Text(
                                text = row.label,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(text = row.value, style = MaterialTheme.typography.bodyMedium)
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
        modifier = modifier,
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
                seriesLine = "Playing Death Games · Volume 1",
                credits = "Yushi Ukai, Nekometaru",
                coverPath = null,
                genres = listOf("Drama", "Fantasy", "Action"),
                synopsis = "Yuki wakes up to find herself in an unfamiliar manor.",
                state = BookState.READING,
                percent = 58,
                pageLabel = "Page 116 of 200",
                continuePage = 116,
                pageCount = "200 pages",
                velocity = "Avg. 42 pages/h",
                timeLeft = "~2h 15m",
                info = emptyList(),
            ),
            onNavigateBack = {},
            onRetry = {},
            onEdit = {},
            onDelete = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
