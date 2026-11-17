package my.naqibrafiqi.pustaka.ui.feature.series.screen.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.centredContentWidth
import my.naqibrafiqi.pustaka.ui.components.BookCover
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import my.naqibrafiqi.pustaka.util.text.asString
import my.naqibrafiqi.pustaka.util.text.uiText

/** The Edit details screen, for a series or a book. */
@Composable
fun EditDetailsScreen(
    title: String,
    state: EditUiState,
    onNavigateBack: () -> Unit,
    onChange: (key: String, value: String) -> Unit,
    onReset: (key: String) -> Unit,
    onUseDetailsFromFile: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onPickCover: (coverPath: String) -> Unit = {},
    onResetCover: () -> Unit = {},
) {
    val ready = state as? EditUiState.Ready
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val leave = { if (ready?.hasChanges == true) confirmDiscard = true else onNavigateBack() }

    BackHandler(enabled = ready?.hasChanges == true) { confirmDiscard = true }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = title,
                subtitle = ready?.subtitle,
                onNavigateBack = leave,
                actionButtons = if (ready == null) null else {
                    {
                        if (ready.isSaving) {
                            val saving = stringResource(R.string.edit_saving)
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp)
                                    .semantics { contentDescription = saving },
                                strokeWidth = 2.dp,
                            )
                        } else {
                            TextButton(onClick = onSave) { Text(stringResource(R.string.common_save)) }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())

        when (state) {
            EditUiState.Loading -> LoadingState(modifier = contentModifier)
            EditUiState.NotFound -> MessageState(
                title = stringResource(R.string.edit_not_found_title),
                message = stringResource(R.string.edit_not_found_message),
                iconRes = R.drawable.edit,
                modifier = contentModifier,
                actionLabel = stringResource(R.string.common_go_back),
                onAction = onNavigateBack,
            )
            is EditUiState.Ready -> LazyColumn(
                modifier = contentModifier
                    .centredContentWidth()
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.edit_explanation),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (state.canUseFileDetails) {
                    item {
                        OutlinedButton(onClick = onUseDetailsFromFile, modifier = Modifier.fillMaxWidth()) {
                            Icon(painter = painterResource(R.drawable.article), contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.edit_use_file_details))
                        }
                    }
                }

                state.error?.let { error ->
                    item {
                        Text(text = stringResource(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                state.cover?.let { cover ->
                    item(key = COVER_KEY) {
                        CoverPicker(choice = cover, onPick = onPickCover, onReset = onResetCover)
                    }
                }

                items(items = state.fields, key = { it.key }) { field ->
                    FieldInput(field = field, onChange = onChange, onReset = onReset)
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.edit_discard_title)) },
            text = { Text(stringResource(R.string.edit_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onNavigateBack()
                }) { Text(stringResource(R.string.edit_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.edit_keep_editing)) }
            },
        )
    }
}

@Composable
private fun FieldInput(
    field: EditField,
    onChange: (String, String) -> Unit,
    onReset: (String) -> Unit,
) {
    OutlinedTextField(
        value = field.value,
        onValueChange = { onChange(field.key, it) },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (field.multiline) Modifier.heightIn(min = 120.dp) else Modifier),
        label = { Text(stringResource(field.label)) },
        singleLine = !field.multiline,
        isError = field.error != null,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = if (field.isNumber) KeyboardType.Decimal else KeyboardType.Text,
        ),
        trailingIcon = if (field.isEdited) {
            {
                IconButton(onClick = { onReset(field.key) }) {
                    Icon(painter = painterResource(R.drawable.undo), contentDescription = stringResource(R.string.edit_reset_field, stringResource(field.label)))
                }
            }
        } else {
            null
        },
        supportingText = supportingText(field),
    )
}

private const val COVER_KEY = "cover"
private val COVER_WIDTH = 88.dp

/** A row of the volumes' covers to choose the series cover from. The chosen one has a check. */
@Composable
private fun CoverPicker(
    choice: CoverChoice,
    onPick: (String) -> Unit,
    onReset: () -> Unit,
) {
    val label = stringResource(R.string.edit_field_cover)
    val selectedIndex = choice.options.indexOfFirst { it.coverPath == choice.selected }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val shape = RoundedCornerShape(8.dp)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (choice.isEdited) {
                IconButton(onClick = onReset) {
                    Icon(
                        painter = painterResource(R.drawable.undo),
                        contentDescription = stringResource(R.string.edit_reset_field, label),
                    )
                }
            }
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
        ) {
            items(items = choice.options, key = { it.coverPath }) { option ->
                val selected = option.coverPath == choice.selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .width(COVER_WIDTH)
                        .clip(shape)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(option.coverPath) }),
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        BookCover(
                            imageUrl = option.coverPath,
                            shape = shape,
                            modifier = if (selected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                            } else {
                                Modifier
                            },
                        )
                        if (selected) {
                            Icon(
                                painter = painterResource(R.drawable.check_small),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                            )
                        }
                    }
                    Text(
                        text = option.label.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        val defaultLabel = choice.options.firstOrNull { it.coverPath == choice.default }?.label?.asString()
        Text(
            text = if (choice.isEdited && defaultLabel != null) {
                stringResource(R.string.edit_default_value, defaultLabel)
            } else {
                stringResource(R.string.edit_hint_cover)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The error, or what the field resets to once it's edited, or its hint. */
private fun supportingText(field: EditField): (@Composable () -> Unit)? {
    val error = field.error
    val hint = field.hint
    if (error == null && !field.isEdited && hint == null) return null
    return {
        val text = when {
            error != null -> stringResource(error)
            field.isEdited && field.default.isNotBlank() -> stringResource(R.string.edit_default_value, field.default.lineSequence().first())
            field.isEdited -> stringResource(R.string.edit_default_empty)
            else -> stringResource(hint!!)
        }
        Text(text = text, maxLines = 1)
    }
}

@Preview(showBackground = true)
@Composable
private fun EditDetailsScreenPreview() {
    PustakaTheme {
        EditDetailsScreen(
            title = "Edit series",
            state = EditUiState.Ready(
                subtitle = "Harbour Tales",
                fields = listOf(
                    EditField("name", R.string.edit_field_name, "Harbour Tales", "[Group] Harbour Tales (Digital)"),
                    EditField("author", R.string.details_author, "Ren Okada", "Ren Okada"),
                    EditField("tags", R.string.edit_field_tags, "Comedy", "Comedy", hint = R.string.edit_hint_tags),
                    EditField("synopsis", R.string.edit_field_synopsis, "Three friends start a club.", "", multiline = true),
                ),
                cover = CoverChoice(
                    options = (1..4).map { CoverOption("cover-$it", uiText(R.string.volume_label, it)) },
                    selected = "cover-2",
                    default = "cover-4",
                ),
                canUseFileDetails = true,
            ),
            onNavigateBack = {},
            onChange = { _, _ -> },
            onReset = {},
            onUseDetailsFromFile = {},
            onSave = {},
        )
    }
}
