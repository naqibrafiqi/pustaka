package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.settings.ReaderFont
import my.naqibrafiqi.pustaka.data.settings.ReaderLineSpacing
import my.naqibrafiqi.pustaka.data.settings.ReaderMargins
import my.naqibrafiqi.pustaka.data.settings.ReaderSettings
import my.naqibrafiqi.pustaka.data.settings.ReaderTheme
import my.naqibrafiqi.pustaka.ui.components.sheetDragArea
import my.naqibrafiqi.pustaka.ui.components.steadyBottomSheet
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import kotlin.math.roundToInt

/**
 * Brightness, theme, font, text size, line spacing, margins, justifying and scrolling. Changes show
 * on the page straight away.
 */
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
        Column(modifier = Modifier.navigationBarsPadding().steadyBottomSheet()) {
            Row(
                modifier = Modifier
                    .sheetDragArea()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.reader_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                TextButton(onClick = onReset, enabled = !settings.looksLikeDefault) { Text(stringResource(R.string.common_reset)) }
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
        Brightness(settings, onChange)
        ThemeChoice(settings.theme) { onChange(settings.copy(theme = it)) }
        Choice(R.string.reader_setting_font, ReaderFont.entries, settings.font, { it.label }) { onChange(settings.copy(font = it)) }
        TextSize(settings, onChange)
        Choice(R.string.reader_setting_line_spacing, ReaderLineSpacing.entries, settings.lineSpacing, { it.label }) {
            onChange(settings.copy(lineSpacing = it))
        }
        Choice(R.string.reader_setting_margins, ReaderMargins.entries, settings.margins, { it.label }) { onChange(settings.copy(margins = it)) }
        SwitchRow(R.string.reader_setting_justify, R.string.reader_setting_justify_description, settings.justify) {
            onChange(settings.copy(justify = it))
        }
        SwitchRow(R.string.reader_setting_scroll, R.string.reader_setting_scroll_description, settings.scroll) {
            onChange(settings.copy(scroll = it))
        }
    }
}

@Composable
private fun SwitchRow(
    @StringRes title: Int,
    @StringRes description: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun <T> Choice(
    @StringRes title: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    icon = {},
                ) {
                    Text(stringResource(label(option)), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * A swatch of each theme's page, with its name underneath. Five themes are too many to name in a
 * row of buttons on a phone, so the swatches show what each looks like instead (#55).
 */
@Composable
private fun ThemeChoice(selected: ReaderTheme, onSelect: (ReaderTheme) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.reader_setting_theme), style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
            ReaderTheme.entries.forEach { theme ->
                val isSelected = theme == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(theme) })
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ThemeSwatch(theme, isSelected)
                    Text(
                        text = stringResource(theme.label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

/** "Aa" in the theme's text colour on its page colour. Auto is half light and half dark. */
@Composable
private fun ThemeSwatch(theme: ReaderTheme, selected: Boolean) {
    val light = ReaderSettings(theme = theme).pageColors(systemDark = false)
    val dark = ReaderSettings(theme = theme).pageColors(systemDark = true)
    val background = if (light == dark) {
        SolidColor(Color(light.background))
    } else {
        // A hard edge down the middle.
        Brush.horizontalGradient(
            0f to Color(light.background),
            0.5f to Color(light.background),
            0.5f to Color(dark.background),
            1f to Color(dark.background),
        )
    }
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = Color(light.text))) { append("A") }
        withStyle(SpanStyle(color = Color(dark.text))) { append("a") }
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.clearAndSetSemantics {})
    }
}

/**
 * The reader's own brightness (#54, D31). Until it's set, it shows the phone's brightness. While
 * the slider is dragged, the screen shows the brightness under the thumb, and letting go saves it.
 */
@Composable
private fun Brightness(settings: ReaderSettings, onChange: (ReaderSettings) -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val saved = settings.brightness ?: remember { ScreenBrightness.phoneLevel(context) } ?: DEFAULT_BRIGHTNESS
    // The level under the thumb while dragging, or null when not dragging.
    var dragLevel by remember { mutableStateOf<Float?>(null) }
    val level = dragLevel ?: saved
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.reader_setting_brightness), style = MaterialTheme.typography.titleSmall)
        val description = stringResource(R.string.reader_setting_brightness)
        val state = stringResource(R.string.common_percent, (level * 100).roundToInt())
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.brightness_low),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Slider(
                value = level,
                onValueChange = {
                    dragLevel = it
                    activity?.let { activity -> ScreenBrightness.apply(activity, it) }
                },
                onValueChangeFinished = {
                    dragLevel?.let { onChange(settings.copy(brightness = it)) }
                    dragLevel = null
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = description
                        stateDescription = state
                    },
            )
            Icon(
                painter = painterResource(R.drawable.brightness_high),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Where the slider starts when the phone's brightness can't be read. */
private const val DEFAULT_BRIGHTNESS = 0.5f

@Composable
private fun TextSize(settings: ReaderSettings, onChange: (ReaderSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.reader_setting_text_size), style = MaterialTheme.typography.titleSmall)
        val smaller = stringResource(R.string.reader_text_smaller)
        val bigger = stringResource(R.string.reader_text_bigger)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { onChange(settings.withTextSize(-1)) },
                enabled = settings.canMakeTextSmaller,
                modifier = Modifier.semantics { contentDescription = smaller },
            ) {
                Text(
                    text = stringResource(R.string.reader_text_size_glyph),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
            Text(
                text = stringResource(R.string.common_percent, settings.textSizePercent),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 64.dp),
            )
            FilledTonalIconButton(
                onClick = { onChange(settings.withTextSize(1)) },
                enabled = settings.canMakeTextBigger,
                modifier = Modifier.semantics { contentDescription = bigger },
            ) {
                Text(
                    text = stringResource(R.string.reader_text_size_glyph),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clearAndSetSemantics {},
                )
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
