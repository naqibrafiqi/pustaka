package my.naqibrafiqi.pustaka.ui.feature.settings.screen

import android.content.ActivityNotFoundException
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import my.naqibrafiqi.pustaka.data.library.access.PickedFolder

/**
 * Returns a function that opens Android's folder picker (decision D15). [onPicked] gets the chosen
 * folder, and isn't called when the picker is closed without choosing one. [onUnavailable] is
 * called on the rare phone without a picker.
 *
 * Pustaka reads the folder through "All files access", so the picker is only used to choose it,
 * and no permission to the folder is kept.
 */
@Composable
fun rememberFolderPicker(onPicked: (PickedFolder) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val currentOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        currentOnPicked(
            PickedFolder.from(
                authority = uri.authority,
                treeDocumentId = documentId,
                internalStorageRoot = Environment.getExternalStorageDirectory().path,
            )
        )
    }
    return {
        try {
            launcher.launch(null)
        } catch (_: ActivityNotFoundException) {
            onUnavailable()
        }
    }
}
