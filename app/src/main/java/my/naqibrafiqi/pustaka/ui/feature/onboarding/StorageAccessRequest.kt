package my.naqibrafiqi.pustaka.ui.feature.onboarding

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import my.naqibrafiqi.pustaka.data.library.access.AndroidStorageAccess

/**
 * Returns a function that asks for access to the files. [onResult] is called when the user comes
 * back, whatever they chose.
 *
 * - Android 11 and newer: opens the system screen where "Allow access to manage all files" is
 *   turned on for Pustaka.
 * - Android 10: asks for the storage permissions. Once Android stops showing that dialog (the user
 *   refused it twice), it opens the app's system settings instead.
 */
@Composable
fun rememberStorageAccessRequest(onResult: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onResult() }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onResult() }
    var hasAskedForPermissions by rememberSaveable { mutableStateOf(false) }

    return request@{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appScreen = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.fromParts("package", context.packageName, null),
            )
            try {
                settingsLauncher.launch(appScreen)
            } catch (_: ActivityNotFoundException) {
                // Some phones only have the list of all apps.
                settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
            return@request
        }

        val activity = context.findActivity()
        val canShowDialog = !hasAskedForPermissions ||
            AndroidStorageAccess.LEGACY_PERMISSIONS.any { activity?.shouldShowRequestPermissionRationale(it) == true }
        if (canShowDialog) {
            hasAskedForPermissions = true
            permissionLauncher.launch(AndroidStorageAccess.LEGACY_PERMISSIONS)
        } else {
            settingsLauncher.launch(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
