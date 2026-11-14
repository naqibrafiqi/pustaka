package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import kotlin.math.roundToInt

/**
 * The bars shown over the page when the middle of the page is tapped: the title and contents at
 * the top, and where you are in the book at the bottom.
 */
@Composable
fun ReaderChrome(
    visible: Boolean,
    title: String,
    position: ReaderPosition,
    onNavigateBack: () -> Unit,
    onOpenContents: () -> Unit,
    onGoToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            ReaderTopBar(
                title = title,
                chapterTitle = position.chapterTitle,
                onNavigateBack = onNavigateBack,
                onOpenContents = onOpenContents,
            )
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            ReaderBottomBar(position = position, onGoToPage = onGoToPage)
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    chapterTitle: String?,
    onNavigateBack: () -> Unit,
    onOpenContents: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Close the book")
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (chapterTitle != null) {
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onOpenContents) {
                Icon(painter = painterResource(R.drawable.list), contentDescription = "Contents")
            }
        }
    }
}

/** The page slider. Dragging shows the page it would go to, and letting go jumps there. */
@Composable
private fun ReaderBottomBar(position: ReaderPosition, onGoToPage: (Int) -> Unit) {
    // The page under the thumb while dragging, or null when not dragging.
    var dragPage by remember { mutableStateOf<Int?>(null) }
    val shownPage = dragPage ?: position.page

    Surface(tonalElevation = 3.dp, shadowElevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (position.pageCount > 1) {
                Slider(
                    value = (shownPage ?: 1).toFloat(),
                    onValueChange = { dragPage = it.roundToInt() },
                    onValueChangeFinished = {
                        dragPage?.let(onGoToPage)
                        dragPage = null
                    },
                    valueRange = 1f..position.pageCount.toFloat(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = shownPage?.let { "Page $it of ${position.pageCount}" } ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (dragPage == null) {
                    Text(
                        text = "${position.percent}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The table of contents, scrolled to the chapter being read. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentsSheet(
    entries: List<TocEntry>,
    currentIndex: Int?,
    onSelect: (TocEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            text = "Contents",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        if (entries.isEmpty()) {
            Text(
                text = "This book doesn't have a table of contents.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            )
        } else {
            ContentsList(entries = entries, currentIndex = currentIndex, onSelect = onSelect)
        }
    }
}

@Composable
private fun ContentsList(entries: List<TocEntry>, currentIndex: Int?, onSelect: (TocEntry) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        // Leaves a chapter above the current one in view, for context.
        currentIndex?.let { listState.scrollToItem((it - 1).coerceAtLeast(0)) }
    }
    LazyColumn(state = listState, modifier = Modifier.navigationBarsPadding()) {
        itemsIndexed(entries) { index, entry ->
            val isCurrent = index == currentIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = "Go to chapter") { onSelect(entry) }
                    .background(if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(start = 24.dp + 16.dp * entry.depth.coerceAtMost(MAX_INDENT), end = 24.dp)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.title,
                    style = if (entry.depth == 0) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (isCurrent) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Reading",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

private const val MAX_INDENT = 3

@Composable
fun ReaderLoading(modifier: Modifier = Modifier) {
    LoadingState(modifier = modifier.background(MaterialTheme.colorScheme.background))
}

@Composable
fun ReaderFailed(message: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        MessageState(
            title = "Couldn't open this book",
            message = message,
            iconRes = R.drawable.broken_image,
            actionLabel = "Close",
            onAction = onClose,
        )
    }
}

@Preview
@Composable
private fun ReaderChromePreview() {
    PustakaTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            ReaderChrome(
                visible = true,
                title = "Brunhild, Vol. 01",
                position = ReaderPosition(page = 42, pageCount = 240, percent = 17, chapterTitle = "Chapter 2: The Dragon's Maiden"),
                onNavigateBack = {},
                onOpenContents = {},
                onGoToPage = {},
            )
        }
    }
}

@Preview
@Composable
private fun ReaderFailedPreview() {
    PustakaTheme {
        ReaderFailed(message = ReaderViewModel.messageFor(my.naqibrafiqi.pustaka.data.library.epub.OpenFailure.PROTECTED), onClose = {})
    }
}
