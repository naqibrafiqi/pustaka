package my.naqibrafiqi.pustaka.ui.feature.reader

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.util.AbsoluteUrl

/**
 * The reader. Readium's reader view is an Android Fragment, so it lives in its own activity, with
 * the menus drawn in Compose on top of it.
 *
 * The activity handles rotation and dark mode itself instead of being recreated, so the book stays
 * open. If Android does recreate it, for example after closing the app in the background, it
 * starts again from the saved position.
 */
@AndroidEntryPoint
class ReaderActivity : AppCompatActivity() {

    private val viewModel: ReaderViewModel by viewModels()

    /** The reader view once it's on screen, for the text selection menu. */
    private var navigatorFragment: EpubNavigatorFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            // Android restores the reader fragment before the book is open again, and it can't
            // be created without the book. Restore an empty one instead and start over.
            supportFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
            super.onCreate(savedInstanceState)
            startActivity(intent(this, intent.getLongExtra(EXTRA_BOOK_ID, 0), fromStart = false))
            finish()
            return
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PustakaTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                when (val current = state) {
                    ReaderUiState.Loading -> ReaderLoading()
                    is ReaderUiState.Failed -> ReaderFailed(message = current.message, onClose = ::finish)
                    is ReaderUiState.Ready -> Reader(current)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onReaderResumed()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onReaderPaused()
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun Reader(book: ReaderUiState.Ready) {
        val settings by viewModel.settings.collectAsStateWithLifecycle()
        val finishCard by viewModel.finishCard.collectAsStateWithLifecycle()
        val systemDark = isSystemInDarkTheme()
        val theme = settings.readiumTheme(systemDark)
        val preferences = settings.toEpubPreferences(systemDark)

        // The fragment is made by Readium's factory, which has to be in place before it's added.
        remember(book) {
            supportFragmentManager.fragmentFactory = EpubNavigatorFactory(book.publication).createFragmentFactory(
                initialLocator = book.initialLocator,
                initialPreferences = preferences,
                listener = linkListener,
                configuration = EpubNavigatorFragment.Configuration().apply {
                    // The page keeps the same size when the system bars hide, so it isn't laid
                    // out again every time the menus are shown.
                    shouldApplyInsetsPadding = false
                    selectionActionModeCallback = selectionMenu
                },
            )
        }

        var navigator by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
        var chromeVisible by rememberSaveable { mutableStateOf(false) }
        var contentsOpen by rememberSaveable { mutableStateOf(false) }
        var settingsOpen by rememberSaveable { mutableStateOf(false) }
        val position by viewModel.position.collectAsStateWithLifecycle()
        val highlights by viewModel.highlights.collectAsStateWithLifecycle()
        val openHighlightId by viewModel.openHighlightId.collectAsStateWithLifecycle()
        val openHighlight = highlights.firstOrNull { it.id == openHighlightId }?.highlight

        SystemBars(
            visible = chromeVisible || contentsOpen || settingsOpen || openHighlight != null,
            darkContent = theme != Theme.DARK,
        )

        // The finish card takes the bottom of the screen, so the menus make way for it.
        LaunchedEffect(finishCard) { if (finishCard != null) chromeVisible = false }
        LaunchedEffect(navigator, preferences) { navigator?.submitPreferences(preferences) }
        LaunchedEffect(navigator) { navigator?.currentLocator?.collect(viewModel::onLocatorChanged) }
        PageTaps(navigator, scroll = settings.scroll, onMenuTap = { chromeVisible = !chromeVisible })
        Highlights(navigator, highlights = highlights, openId = openHighlightId, scroll = settings.scroll)

        Box(
            Modifier
                .fillMaxSize()
                .background(Color(theme.backgroundColor))
        ) {
            AndroidFragment<EpubNavigatorFragment>(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout)),
                onUpdate = {
                    navigator = it
                    navigatorFragment = it
                },
            )

            ReaderChrome(
                visible = chromeVisible,
                title = book.title,
                position = position,
                onNavigateBack = ::finish,
                onOpenContents = { contentsOpen = true },
                onOpenSettings = { settingsOpen = true },
                onGoToPage = { page -> book.positions.getOrNull(page - 1)?.let { navigator?.go(it, animated = false) } },
            )

            finishCard?.let { card ->
                FinishCardView(
                    card = card,
                    onReadNext = { next ->
                        startActivity(intent(this@ReaderActivity, next.bookId, next.fromStart))
                        finish()
                    },
                    onClose = viewModel::dismissFinishCard,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        openHighlight?.let { highlight ->
            HighlightSheet(
                highlight = highlight,
                onChange = { color, note -> viewModel.updateHighlight(highlight.id, color, note) },
                onDelete = { viewModel.deleteHighlight(highlight.id) },
                onDismiss = viewModel::closeHighlight,
            )
        }

        if (settingsOpen) {
            ReaderSettingsSheet(
                settings = settings,
                onChange = viewModel::updateSettings,
                onReset = viewModel::resetSettings,
                onDismiss = { settingsOpen = false },
            )
        }

        if (contentsOpen) {
            ContentsSheet(
                entries = book.tableOfContents,
                currentIndex = position.tocIndex,
                onSelect = { entry ->
                    contentsOpen = false
                    chromeVisible = false
                    navigator?.go(entry.link, animated = false)
                },
                onDismiss = { contentsOpen = false },
            )
        }
    }

    /**
     * Taps on the edges turn the page. A tap in the middle shows or hides the menus. When
     * scrolling, there are no pages to turn, so any tap shows or hides the menus.
     */
    @Composable
    private fun PageTaps(navigator: EpubNavigatorFragment?, scroll: Boolean, onMenuTap: () -> Unit) {
        val latestOnMenuTap by rememberUpdatedState(onMenuTap)
        val latestScroll by rememberUpdatedState(scroll)
        DisposableEffect(navigator) {
            if (navigator == null) return@DisposableEffect onDispose { }
            val listener = object : InputListener {
                override fun onTap(event: TapEvent): Boolean {
                    if (latestScroll) {
                        latestOnMenuTap()
                        return true
                    }
                    val rightToLeft = navigator.settings.value.readingProgression == ReadingProgression.RTL
                    when (TapZone.of(event.point.x, navigator.publicationView.width, rightToLeft)) {
                        TapZone.PREVIOUS -> navigator.goBackward(animated = true)
                        TapZone.NEXT -> navigator.goForward(animated = true)
                        TapZone.MENU -> latestOnMenuTap()
                    }
                    return true
                }
            }
            navigator.addInputListener(listener)
            onDispose { navigator.removeInputListener(listener) }
        }
    }

    /**
     * Draws the book's highlights on the page, and opens one when it's tapped. The open one is
     * drawn as active, so it stands out while its sheet is showing.
     */
    @Composable
    private fun Highlights(
        navigator: EpubNavigatorFragment?,
        highlights: List<ReaderHighlight>,
        openId: Long?,
        scroll: Boolean,
    ) {
        val latestScroll by rememberUpdatedState(scroll)
        LaunchedEffect(navigator, highlights, openId) {
            val decorations = highlights.map {
                Decoration(
                    id = it.id.toString(),
                    locator = it.locator,
                    style = Decoration.Style.Highlight(tint = it.highlight.color.argb, isActive = it.id == openId),
                )
            }
            navigator?.applyDecorations(decorations, HIGHLIGHTS)
        }
        DisposableEffect(navigator) {
            if (navigator == null) return@DisposableEffect onDispose { }
            val listener = object : DecorableNavigator.Listener {
                override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                    val id = event.decoration.id.toLongOrNull() ?: return false
                    val x = event.point?.x ?: return false
                    val rightToLeft = navigator.settings.value.readingProgression == ReadingProgression.RTL
                    // Not handled here means the tap goes on to PageTaps and turns the page.
                    if (!highlightTapOpens(x, navigator.publicationView.width, latestScroll, rightToLeft)) return false
                    viewModel.openHighlight(id)
                    return true
                }
            }
            navigator.addDecorationListener(HIGHLIGHTS, listener)
            onDispose { navigator.removeDecorationListener(listener) }
        }
    }

    /**
     * The menu shown over selected text. Readium replaces the system's menu with this one, so Copy
     * and Share are added back next to Highlight.
     */
    private val selectionMenu = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.add(Menu.NONE, MENU_HIGHLIGHT, 0, "Highlight")
            menu.add(Menu.NONE, MENU_COPY, 1, android.R.string.copy)
            menu.add(Menu.NONE, MENU_SHARE, 2, "Share")
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val navigator = navigatorFragment ?: return false
            lifecycleScope.launch {
                val selection = navigator.currentSelection()
                navigator.clearSelection()
                mode.finish()
                val locator = selection?.locator ?: return@launch
                val text = locator.text.highlight?.trim()?.ifEmpty { null } ?: return@launch
                when (item.itemId) {
                    MENU_HIGHLIGHT -> viewModel.addHighlight(locator)
                    MENU_COPY -> copy(text)
                    MENU_SHARE -> share(text)
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {}
    }

    private fun copy(text: String) {
        // Android shows its own message that the text was copied.
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Quote", text))
    }

    private fun share(text: String) {
        val title = (viewModel.state.value as? ReaderUiState.Ready)?.title
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, shareText(text, title))
        try {
            startActivity(Intent.createChooser(send, null))
        } catch (_: ActivityNotFoundException) {
            // Nothing to share with.
        }
    }

    /** Hides the status and navigation bars while reading. A swipe from the edge shows them briefly. */
    @Composable
    private fun SystemBars(visible: Boolean, darkContent: Boolean) {
        LaunchedEffect(visible, darkContent) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.isAppearanceLightStatusBars = darkContent
            controller.isAppearanceLightNavigationBars = darkContent
            if (visible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    /** Links to websites in the book open in the browser. */
    private val linkListener = object : EpubNavigatorFragment.Listener {
        override fun onExternalLinkActivated(url: AbsoluteUrl) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url.toString().toUri()))
            } catch (_: ActivityNotFoundException) {
                // No browser installed, so the link does nothing.
            }
        }
    }

    companion object {
        const val EXTRA_BOOK_ID = "bookId"
        const val EXTRA_FROM_START = "fromStart"
        const val EXTRA_LOCATOR = "locator"

        /** The reader's group of decorations for highlights. */
        private const val HIGHLIGHTS = "highlights"
        private const val MENU_HIGHLIGHT = 1
        private const val MENU_COPY = 2
        private const val MENU_SHARE = 3

        /**
         * @param fromStart forgets the saved position and opens the book at its first page.
         *   Otherwise the book opens where it was left.
         * @param locator a place in the book to open at instead, such as a highlight's, as a
         *   serialized Readium `Locator`.
         */
        fun intent(context: Context, bookId: Long, fromStart: Boolean = false, locator: String? = null): Intent =
            Intent(context, ReaderActivity::class.java)
                .putExtra(EXTRA_BOOK_ID, bookId)
                .putExtra(EXTRA_FROM_START, fromStart)
                .putExtra(EXTRA_LOCATOR, locator)
    }
}
