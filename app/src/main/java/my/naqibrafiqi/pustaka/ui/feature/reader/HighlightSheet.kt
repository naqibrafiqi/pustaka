package my.naqibrafiqi.pustaka.ui.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import my.naqibrafiqi.pustaka.R
import my.naqibrafiqi.pustaka.data.model.Highlight
import my.naqibrafiqi.pustaka.data.model.HighlightColor
import my.naqibrafiqi.pustaka.ui.theme.PustakaTheme
import java.time.Instant

/**
 * Opens when a highlight on the page is tapped: change its colour, write a note, or delete it.
 * A new colour shows on the page straight away. The note is saved when the sheet closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightSheet(
    highlight: Highlight,
    onChange: (color: HighlightColor, note: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var note by rememberSaveable(highlight.id) { mutableStateOf(highlight.note.orEmpty()) }
    var confirmDelete by rememberSaveable(highlight.id) { mutableStateOf(false) }

    // The colour is saved as soon as it's picked, so only the note is left to save.
    fun saveAndDismiss() {
        onChange(highlight.color, note)
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = ::saveAndDismiss, sheetState = sheetState) {
        HighlightSheetContent(
            highlight = highlight,
            note = note,
            onNoteChange = { note = it },
            onColorChange = { onChange(it, note) },
            onDelete = {
                // A highlight with a note asks first, so the note isn't lost by mistake.
                if (highlight.note.isNullOrBlank() && note.isBlank()) onDelete() else confirmDelete = true
            },
            onDone = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { saveAndDismiss() }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this highlight?") },
            text = { Text("Its note is deleted too.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HighlightSheetContent(
    highlight: Highlight,
    note: String,
    onNoteChange: (String) -> Unit,
    onColorChange: (HighlightColor) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Highlight", style = MaterialTheme.typography.titleLarge)

        Quote(text = highlight.text, color = highlight.color)

        ColorPicker(selected = highlight.color, onSelect = onColorChange)

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text("Note") },
            placeholder = { Text("Add a note") },
            minLines = 2,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Delete")
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onDone) { Text("Done") }
        }
    }
}

/** The highlighted text, with a bar in the highlight's colour beside it. */
@Composable
private fun Quote(text: String, color: HighlightColor) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.small)
                .background(Color(color.argb))
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ColorPicker(selected: HighlightColor, onSelect: (HighlightColor) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HighlightColor.entries.forEach { color ->
            val isSelected = color == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(color.argb))
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(color) }
                    .semantics { contentDescription = color.label },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HighlightSheetContentPreview() {
    PustakaTheme {
        HighlightSheetContent(
            highlight = Highlight(
                id = 1,
                bookId = 1,
                locator = "{}",
                text = "The dragon's maiden looked up at the sky and, for the first time in a thousand years, smiled.",
                note = null,
                color = HighlightColor.GREEN,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            note = "",
            onNoteChange = {},
            onColorChange = {},
            onDelete = {},
            onDone = {},
        )
    }
}
