package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.data.settings.ReaderFont
import my.naqibrafiqi.pustaka.data.settings.ReaderLineSpacing
import my.naqibrafiqi.pustaka.data.settings.ReaderMargins
import my.naqibrafiqi.pustaka.data.settings.ReaderSettings
import my.naqibrafiqi.pustaka.data.settings.ReaderTheme
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

/** Theme, font, text size, line spacing, margins and scrolling. Changes show on the page straight away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onChange: (ReaderSettings) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // The page stays visible behind the sheet, so changes can be seen as they're made.
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.1f),
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Reading settings",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onReset, enabled = settings != ReaderSettings()) { Text("Reset") }
            }
            ReaderSettingsForm(settings, onChange, Modifier.padding(top = 20.dp))
        }
    }
}

/**
 * Every reader setting. Used in the reader's sheet and in Settings → Reader settings, which edit
 * the same set (decision D8).
 */
@Composable
fun ReaderSettingsForm(
    settings: ReaderSettings,
    onChange: (ReaderSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {

        Choice("Theme", ReaderTheme.entries, settings.theme, { it.label }) { onChange(settings.copy(theme = it)) }
        Choice("Font", ReaderFont.entries, settings.font, { it.label }) { onChange(settings.copy(font = it)) }
        TextSize(settings, onChange)
        Choice("Line spacing", ReaderLineSpacing.entries, settings.lineSpacing, { it.label }) {
            onChange(settings.copy(lineSpacing = it))
        }
        Choice("Margins", ReaderMargins.entries, settings.margins, { it.label }) { onChange(settings.copy(margins = it)) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = settings.scroll,
                    role = Role.Switch,
                    onValueChange = { onChange(settings.copy(scroll = it)) },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Scroll", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Scroll through each chapter instead of turning pages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = settings.scroll, onCheckedChange = null)
        }
    }
}

@Composable
private fun <T> Choice(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    icon = {},
                ) {
                    Text(label(option), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun TextSize(settings: ReaderSettings, onChange: (ReaderSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Text size", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { onChange(settings.withTextSize(-1)) },
                enabled = settings.canMakeTextSmaller,
                modifier = Modifier.semantics { contentDescription = "Smaller text" },
            ) {
                Text("A", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = settings.textSizeLabel,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 64.dp),
            )
            FilledTonalIconButton(
                onClick = { onChange(settings.withTextSize(1)) },
                enabled = settings.canMakeTextBigger,
                modifier = Modifier.semantics { contentDescription = "Bigger text" },
            ) {
                Text("A", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview
@Composable
private fun ReaderSettingsPreview() {
    PustakaTheme {
        Surface {
            ReaderSettingsForm(
                settings = ReaderSettings(theme = ReaderTheme.SEPIA, textSize = 1.2, scroll = true),
                onChange = {},
            )
        }
    }
}
