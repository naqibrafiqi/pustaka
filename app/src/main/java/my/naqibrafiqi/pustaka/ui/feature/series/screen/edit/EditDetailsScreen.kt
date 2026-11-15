package my.naqibrafiqi.pustaka.ui.feature.series.screen.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.ui.components.LoadingState
import my.naqibrafiqi.pustaka.ui.components.MessageState
import my.naqibrafiqi.pustaka.ui.components.TopBar
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme

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
                modifier = contentModifier.imePadding(),
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
