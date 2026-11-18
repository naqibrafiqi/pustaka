package my.naqibrafiqi.pustaka.ui.feature.settings.screen.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.settings.ReaderSettings
import my.naqibrafiqi.pustaka.data.settings.ReaderSettingsRepository
import my.naqibrafiqi.pustaka.ui.components.centredContentWidth
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.feature.reader.ReaderSettingsForm
import my.naqibrafiqi.pustaka.ui.feature.reader.ScreenBrightness
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import javax.inject.Inject

/** Settings → Reader settings. The same set the reader's own sheet changes (decision D8). */
@HiltViewModel
class ReaderDefaultsViewModel @Inject constructor(
    private val repository: ReaderSettingsRepository,
) : ViewModel() {

    val settings: StateFlow<ReaderSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun update(settings: ReaderSettings) {
        viewModelScope.launch { repository.update(settings) }
    }

    fun reset() {
        viewModelScope.launch { repository.reset() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun ReaderDefaultsScreen(
    settings: ReaderSettings?,
    onChange: (ReaderSettings) -> Unit,
    onReset: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    // Dragging the brightness slider shows the brightness on this screen too. The phone's own
    // brightness comes back when the screen is left.
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        onDispose { activity?.let { ScreenBrightness.apply(it, null) } }
    }
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.settings_reader_settings),
                onNavigateBack = onNavigateBack,
                actionButtons = {
                    TextButton(onClick = onReset, enabled = settings != null && !settings.looksLikeDefault) {
                        Text(stringResource(R.string.common_reset))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .centredContentWidth(),
        ) {
            Text(
                text = stringResource(R.string.reader_defaults_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp),
            )
            if (settings != null) ReaderSettingsForm(settings, onChange)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReaderDefaultsScreenPreview() {
    PustakaTheme {
        ReaderDefaultsScreen(settings = ReaderSettings(), onChange = {}, onReset = {}, onNavigateBack = {})
    }
}
