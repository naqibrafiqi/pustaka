package my.naqibrafiqi.pustaka.data.library.access

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Whether the app can read the library folder. Pustaka is distributed outside the Play Store, so it
 * uses "All files access" (`MANAGE_EXTERNAL_STORAGE`) on Android 11 and newer, and the older
 * storage permissions on Android 10.
 */
interface StorageAccess {
    fun hasAccess(): Boolean
}

class AndroidStorageAccess @Inject constructor(
    @ApplicationContext private val context: Context,
) : StorageAccess {

    override fun hasAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            LEGACY_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }

    companion object {
        /** What Android 10 asks for instead of "All files access". */
        val LEGACY_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }
}
