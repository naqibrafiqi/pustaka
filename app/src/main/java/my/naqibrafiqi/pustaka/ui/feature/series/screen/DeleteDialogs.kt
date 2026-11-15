package my.naqibrafiqi.pustaka.ui.feature.series.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import my.naqibrafiqi.pustaka.R
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
            title = stringResource(R.string.delete_named_title, dialog.name),
            choices = listOf(
                DialogChoice(
                    label = stringResource(R.string.delete_remove_from_library),
                    description = stringResource(R.string.delete_series_remove_description),
                    onClick = onRemoveFromLibrary,
                ),
                DialogChoice(
                    label = stringResource(R.string.delete_series_files),
                    description = stringResource(R.string.delete_series_files_description, filesLabel(dialog.contents.files)),
                    isDestructive = true,
                    onClick = onChooseDeleteFiles,
                ),
            ),
            onDismiss = onDismiss,
        )
        is SeriesDeleteDialog.ConfirmDeleteFiles -> {
            val others = dialog.contents.files - dialog.contents.epubs
            val epubs = pluralStringResource(R.plurals.delete_epubs, dialog.contents.epubs, dialog.contents.epubs)
            val what = if (others > 0) {
                stringResource(R.string.delete_epubs_and_others, epubs, pluralStringResource(R.plurals.delete_other_files, others, others))
            } else {
                epubs
            }
            ConfirmDialog(
                title = stringResource(R.string.delete_files_title, filesLabel(dialog.contents.files)),
                message = stringResource(R.string.delete_series_files_message, dialog.folderPath, what),
                confirmLabel = stringResource(R.string.common_delete),
                onConfirm = onDeleteFolder,
                onDismiss = onDismiss,
            )
        }
        is SeriesDeleteDialog.ConfirmDeleteEmptyFolder -> ConfirmDialog(
            title = stringResource(R.string.delete_named_title, dialog.name),
            message = stringResource(R.string.delete_empty_folder_message, dialog.folderPath),
            confirmLabel = stringResource(R.string.delete_folder),
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
            title = stringResource(R.string.delete_named_title, dialog.name),
            choices = listOf(
                DialogChoice(
                    label = stringResource(R.string.delete_remove_from_library),
                    description = stringResource(R.string.delete_book_remove_description),
                    onClick = onRemoveFromLibrary,
                ),
                DialogChoice(
                    label = stringResource(R.string.delete_file),
                    description = stringResource(R.string.delete_file_description, dialog.fileName),
                    isDestructive = true,
                    onClick = onChooseDeleteFile,
                ),
            ),
            onDismiss = onDismiss,
        )
        is BookDeleteDialog.ConfirmDeleteFile -> ConfirmDialog(
            title = stringResource(R.string.delete_file_title),
            message = stringResource(R.string.delete_file_message, dialog.fileName),
            confirmLabel = stringResource(R.string.common_delete),
            onConfirm = onDeleteFile,
            onDismiss = onDismiss,
        )
        is BookDeleteDialog.ConfirmForget -> ConfirmDialog(
            title = stringResource(R.string.delete_forget_title, dialog.name),
            message = stringResource(R.string.delete_forget_message, dialog.fileName),
            confirmLabel = stringResource(R.string.delete_forget_confirm),
            onConfirm = onForget,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun filesLabel(count: Int): String = pluralStringResource(R.plurals.delete_files, count, count)
