package my.naqibrafiqi.pustaka.ui.feature.reader

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
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

    override fun onPause() {
        super.onPause()
        viewModel.saveNow()
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun Reader(book: ReaderUiState.Ready) {
        val theme = if (isSystemInDarkTheme()) Theme.DARK else Theme.LIGHT
        val preferences = EpubPreferences(theme = theme)

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
                },
            )
        }

        var navigator by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
        var chromeVisible by rememberSaveable { mutableStateOf(false) }
        var contentsOpen by rememberSaveable { mutableStateOf(false) }
        val position by viewModel.position.collectAsStateWithLifecycle()

        SystemBars(visible = chromeVisible || contentsOpen, darkContent = theme != Theme.DARK)

        LaunchedEffect(navigator, preferences) { navigator?.submitPreferences(preferences) }
        LaunchedEffect(navigator) { navigator?.currentLocator?.collect(viewModel::onLocatorChanged) }
        PageTaps(navigator, onMenuTap = { chromeVisible = !chromeVisible })

        Box(
            Modifier
                .fillMaxSize()
                .background(Color(theme.backgroundColor))
        ) {
            AndroidFragment<EpubNavigatorFragment>(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout)),
                onUpdate = { navigator = it },
            )

            ReaderChrome(
                visible = chromeVisible,
                title = book.title,
                position = position,
                onNavigateBack = ::finish,
                onOpenContents = { contentsOpen = true },
                onGoToPage = { page -> book.positions.getOrNull(page - 1)?.let { navigator?.go(it, animated = false) } },
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

    /** Taps on the edges turn the page. A tap in the middle shows or hides the menus. */
    @Composable
    private fun PageTaps(navigator: EpubNavigatorFragment?, onMenuTap: () -> Unit) {
        val latestOnMenuTap by rememberUpdatedState(onMenuTap)
        DisposableEffect(navigator) {
            if (navigator == null) return@DisposableEffect onDispose { }
            val listener = object : InputListener {
                override fun onTap(event: TapEvent): Boolean {
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

        /**
         * @param fromStart forgets the saved position and opens the book at its first page.
         *   Otherwise the book opens where it was left.
         */
        fun intent(context: Context, bookId: Long, fromStart: Boolean = false): Intent =
            Intent(context, ReaderActivity::class.java)
                .putExtra(EXTRA_BOOK_ID, bookId)
                .putExtra(EXTRA_FROM_START, fromStart)
    }
}
