package my.naqibrafiqi.pustaka.ui.feature.series.screen

import androidx.compose.runtime.Composable
import my.naqibrafiqi.pustaka.ui.components.ChoiceDialog
import my.naqibrafiqi.pustaka.ui.components.ConfirmDialog
import my.naqibrafiqi.pustaka.ui.components.DialogChoice
import my.naqibrafiqi.pustaka.ui.feature.series.screen.book.details.BookDeleteDialog
import my.naqibrafiqi.pustaka.ui.feature.series.screen.series.details.SeriesDeleteDialog

/** The dialogs for "Delete series". Shows nothing when [dialog] is null. */
@Composable
fun SeriesDeleteDialogs(
    dialog: SeriesDeleteDialog?,
    onRemoveFromLibrary: () -> Unit,
    onChooseDeleteFiles: () -> Unit,
    onDeleteFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit
        is SeriesDeleteDialog.Choose -> ChoiceDialog(
            title = "Delete \"${dialog.name}\"?",
            choices = listOf(
                DialogChoice(
                    label = "Remove from library",
                    description = "Hides this series and its volumes. The folder stays on your phone, " +
                        "and the next scan won't add it back.",
                    onClick = onRemoveFromLibrary,
                ),
                DialogChoice(
                    label = "Delete folder and files",
                    description = "Deletes the folder and the ${filesLabel(dialog.contents.files)} in it " +
                        "from your phone.",
                    isDestructive = true,
                    onClick = onChooseDeleteFiles,
                ),
            ),
            onDismiss = onDismiss,
        )
        is SeriesDeleteDialog.ConfirmDeleteFiles -> {
            val others = dialog.contents.files - dialog.contents.epubs
            val what = buildString {
                append(epubsLabel(dialog.contents.epubs))
                if (others > 0) append(" and ${if (others == 1) "1 other file" else "$others other files"}")
            }
            ConfirmDialog(
                title = "Delete ${filesLabel(dialog.contents.files)}?",
                message = "This deletes the folder ${dialog.folderPath} and everything in it: $what. " +
                    "Reading progress and highlights for these books are deleted too. This can't be undone.",
                confirmLabel = "Delete",
                onConfirm = onDeleteFolder,
                onDismiss = onDismiss,
            )
        }
        is SeriesDeleteDialog.ConfirmDeleteEmptyFolder -> ConfirmDialog(
            title = "Delete \"${dialog.name}\"?",
            message = "This series has no files. Its empty folder ${dialog.folderPath} will be deleted.",
            confirmLabel = "Delete folder",
            onConfirm = onDeleteFolder,
            onDismiss = onDismiss,
        )
    }
}

/** The dialogs for "Delete book". Shows nothing when [dialog] is null. */
@Composable
fun BookDeleteDialogs(
    dialog: BookDeleteDialog?,
    onRemoveFromLibrary: () -> Unit,
    onChooseDeleteFile: () -> Unit,
    onDeleteFile: () -> Unit,
    onForget: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit
        is BookDeleteDialog.Choose -> ChoiceDialog(
            title = "Delete \"${dialog.name}\"?",
            choices = listOf(
                DialogChoice(
                    label = "Remove from library",
                    description = "Hides this book. The file stays on your phone, and the next scan " +
                        "won't add it back.",
                    onClick = onRemoveFromLibrary,
                ),
                DialogChoice(
                    label = "Delete file",
                    description = "Deletes ${dialog.fileName} from your phone.",
                    isDestructive = true,
                    onClick = onChooseDeleteFile,
                ),
            ),
            onDismiss = onDismiss,
        )
        is BookDeleteDialog.ConfirmDeleteFile -> ConfirmDialog(
            title = "Delete file?",
            message = "${dialog.fileName} will be deleted from your phone, with the book's reading " +
                "progress and highlights. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = onDeleteFile,
            onDismiss = onDismiss,
        )
        is BookDeleteDialog.ConfirmForget -> ConfirmDialog(
            title = "Remove \"${dialog.name}\"?",
            message = "${dialog.fileName} can't be found. Removing the book deletes its reading " +
                "progress, highlights and edited details. This can't be undone.",
            confirmLabel = "Remove",
            onConfirm = onForget,
            onDismiss = onDismiss,
        )
    }
}

private fun filesLabel(count: Int): String = if (count == 1) "1 file" else "$count files"

private fun epubsLabel(count: Int): String = if (count == 1) "1 EPUB" else "$count EPUBs"
