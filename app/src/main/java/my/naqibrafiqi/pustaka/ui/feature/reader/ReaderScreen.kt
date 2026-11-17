package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.components.sheetDragArea
import my.naqibrafiqi.pustaka.ui.components.steadyBottomSheet
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asString
import my.naqibrafiqi.pustaka.util.text.asUiText
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
    onOpenSettings: () -> Unit,
    onGoToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    returnPoint: ReturnPoint<*>? = null,
    onReturn: () -> Unit = {},
    bookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
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
                bookmarked = bookmarked,
                onNavigateBack = onNavigateBack,
                onToggleBookmark = onToggleBookmark,
                onOpenContents = onOpenContents,
                onOpenSettings = onOpenSettings,
            )
        }

        // "Back to page 42" comes and goes with the bars, sitting just above the bottom one.
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Keeps the page showing while the button fades out.
                var lastReturnPoint by remember { mutableStateOf(returnPoint) }
                if (returnPoint != null) lastReturnPoint = returnPoint
                AnimatedVisibility(
                    visible = returnPoint != null,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    ReturnButton(
                        page = lastReturnPoint?.page,
                        onClick = onReturn,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                ReaderBottomBar(position = position, onGoToPage = onGoToPage)
            }
        }
    }
}

/** "Back to page 42": goes back to where reading was before a jump (#35). */
@Composable
private fun ReturnButton(page: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(painter = painterResource(R.drawable.undo), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = if (page != null) stringResource(R.string.reader_back_to_page, page) else stringResource(R.string.reader_back_to_previous),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    chapterTitle: String?,
    bookmarked: Boolean,
    onNavigateBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenContents: () -> Unit,
    onOpenSettings: () -> Unit,
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
                Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.reader_close_book))
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
            // Filled when the page is bookmarked (#37).
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    painter = painterResource(if (bookmarked) R.drawable.bookmark_filled else R.drawable.bookmark),
                    contentDescription = stringResource(if (bookmarked) R.string.reader_remove_bookmark else R.string.reader_bookmark_page),
                    tint = if (bookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
            IconButton(onClick = onOpenContents) {
                Icon(painter = painterResource(R.drawable.list), contentDescription = stringResource(R.string.reader_contents))
            }
            IconButton(onClick = onOpenSettings) {
                Icon(painter = painterResource(R.drawable.settings), contentDescription = stringResource(R.string.reader_settings_title))
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
                val pageDescription = stringResource(R.string.reader_page_slider)
                val pageState = stringResource(R.string.page_of_total, shownPage ?: 1, position.pageCount)
                Slider(
                    modifier = Modifier.semantics {
                        contentDescription = pageDescription
                        stateDescription = pageState
                    },
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
                    text = shownPage?.let { stringResource(R.string.page_of_total, it, position.pageCount) }.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (dragPage == null) {
                    Text(
                        text = stringResource(R.string.common_percent, position.percent),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The two tabs of the Contents sheet (D27). */
enum class ContentsTab { CHAPTERS, BOOKMARKS }

/**
 * The table of contents, scrolled to the chapter being read, and the book's bookmarks in a second
 * tab (D27).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentsSheet(
    entries: List<TocEntry>,
    currentIndex: Int?,
    onSelect: (TocEntry) -> Unit,
    onDismiss: () -> Unit,
    bookmarks: List<ReaderBookmark> = emptyList(),
    currentPage: Int? = null,
    tab: ContentsTab = ContentsTab.CHAPTERS,
    onTabChange: (ContentsTab) -> Unit = {},
    onSelectBookmark: (ReaderBookmark) -> Unit = {},
    onDeleteBookmark: (ReaderBookmark) -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.steadyBottomSheet()) {
            Text(
                text = stringResource(R.string.reader_contents),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .sheetDragArea()
                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
            ContentsTabs(tab = tab, bookmarkCount = bookmarks.size, onTabChange = onTabChange)
            when (tab) {
                ContentsTab.CHAPTERS -> if (entries.isEmpty()) {
                    SheetMessage(stringResource(R.string.reader_no_contents))
                } else {
                    ContentsList(entries = entries, currentIndex = currentIndex, onSelect = onSelect)
                }
                ContentsTab.BOOKMARKS -> if (bookmarks.isEmpty()) {
                    SheetMessage(stringResource(R.string.reader_no_bookmarks))
                } else {
                    BookmarksList(
                        bookmarks = bookmarks,
                        currentPage = currentPage,
                        onSelect = onSelectBookmark,
                        onDelete = onDeleteBookmark,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentsTabs(tab: ContentsTab, bookmarkCount: Int, onTabChange: (ContentsTab) -> Unit) {
    PrimaryTabRow(
        selectedTabIndex = tab.ordinal,
        containerColor = Color.Transparent,
        modifier = Modifier.sheetDragArea(),
    ) {
        Tab(
            selected = tab == ContentsTab.CHAPTERS,
            onClick = { onTabChange(ContentsTab.CHAPTERS) },
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            text = { Text(stringResource(R.string.reader_chapters)) },
        )
        Tab(
            selected = tab == ContentsTab.BOOKMARKS,
            onClick = { onTabChange(ContentsTab.BOOKMARKS) },
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            text = {
                Text(
                    if (bookmarkCount > 0) {
                        stringResource(R.string.reader_bookmarks_count, bookmarkCount)
                    } else {
                        stringResource(R.string.reader_bookmarks)
                    }
                )
            },
        )
    }
}

/** A line of text in place of an empty list. */
@Composable
private fun SheetMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .sheetDragArea()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    )
}

/** A list that fits the sheet has nothing to stretch when it's swiped (#31). */
@Composable
private fun rememberSheetOverscroll(listState: LazyListState): OverscrollEffect? {
    val scrollable by remember { derivedStateOf { listState.canScrollForward || listState.canScrollBackward } }
    return if (scrollable) rememberOverscrollEffect() else null
}

@Composable
private fun ContentsList(entries: List<TocEntry>, currentIndex: Int?, onSelect: (TocEntry) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        // Leaves a chapter above the current one in view, for context.
        currentIndex?.let { listState.scrollToItem((it - 1).coerceAtLeast(0)) }
    }
    LazyColumn(
        state = listState,
        overscrollEffect = rememberSheetOverscroll(listState),
        modifier = Modifier.navigationBarsPadding(),
    ) {
        itemsIndexed(entries) { index, entry ->
            val isCurrent = index == currentIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.reader_go_to_chapter)) { onSelect(entry) }
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
                        text = stringResource(R.string.book_state_reading),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

/** The book's bookmarks in book order (#37). The one for the page being read stands out. */
@Composable
private fun BookmarksList(
    bookmarks: List<ReaderBookmark>,
    currentPage: Int?,
    onSelect: (ReaderBookmark) -> Unit,
    onDelete: (ReaderBookmark) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        overscrollEffect = rememberSheetOverscroll(listState),
        modifier = Modifier.navigationBarsPadding(),
    ) {
        items(bookmarks, key = { it.id }) { bookmark ->
            BookmarkRow(
                bookmark = bookmark,
                isCurrent = bookmark.page != null && bookmark.page == currentPage,
                onSelect = { onSelect(bookmark) },
                onDelete = { onDelete(bookmark) },
            )
        }
    }
}

/** The chapter, the page and how far into the book, then the start of the page's text. */
@Composable
private fun BookmarkRow(bookmark: ReaderBookmark, isCurrent: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    val page = bookmark.page?.let { stringResource(R.string.page_number, it) }
    val percent = stringResource(R.string.common_percent, bookmark.percent)
    val textColor = if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    val subtleColor = if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.reader_go_to_bookmark), onClick = onSelect)
            .background(if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = bookmark.chapterTitle ?: page ?: percent,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (page != null) stringResource(R.string.reader_bookmark_place, page, percent) else percent,
                style = MaterialTheme.typography.labelMedium,
                color = subtleColor,
            )
            bookmark.text?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.bookmark_remove),
                contentDescription = stringResource(R.string.reader_remove_bookmark),
                tint = subtleColor,
            )
        }
    }
}

private const val MAX_INDENT = 3

/**
 * Shown on the last page (decision D10): the book is finished, and the next volume is offered
 * when there is one. Nothing opens until a button is tapped.
 */
@Composable
fun FinishCardView(
    card: FinishCard,
    onReadNext: (NextVolume) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.book_state_finished),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = card.title, style = MaterialTheme.typography.titleLarge)
            val next = card.next
            Text(
                text = when {
                    next != null -> stringResource(R.string.reader_finish_up_next, next.label.asString(), next.title)
                    card.seriesName != null -> stringResource(R.string.reader_finish_last_volume, card.seriesName)
                    else -> stringResource(R.string.reader_finish_end_of_book)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onClose) { Text(stringResource(R.string.common_close)) }
                if (next != null) {
                    Button(onClick = { onReadNext(next) }) { Text(stringResource(R.string.reader_read_next)) }
                }
            }
        }
    }
}

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
            title = stringResource(R.string.reader_error_title),
            message = message,
            iconRes = R.drawable.broken_image,
            actionLabel = stringResource(R.string.common_close),
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
                onOpenSettings = {},
                onGoToPage = {},
                returnPoint = ReturnPoint(Unit, page = 12),
                bookmarked = true,
            )
        }
    }
}

@Preview
@Composable
private fun FinishCardPreview() {
    PustakaTheme {
        FinishCardView(
            card = FinishCard(
                title = "Brunhild, Vol. 01",
                seriesName = "Brunhild",
                next = NextVolume(bookId = 2, title = "Brunhild, Vol. 02", label = "Volume 2".asUiText(), fromStart = false),
            ),
            onReadNext = {},
            onClose = {},
        )
    }
}

@Preview
@Composable
private fun ReaderFailedPreview() {
    PustakaTheme {
        ReaderFailed(message = stringResource(ReaderViewModel.messageFor(my.naqibrafiqi.pustaka.data.library.epub.OpenFailure.PROTECTED)), onClose = {})
    }
}
