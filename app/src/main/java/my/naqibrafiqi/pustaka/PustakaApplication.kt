package my.naqibrafiqi.pustaka

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import my.naqibrafiqi.pustaka.data.library.access.StorageAccess
import my.naqibrafiqi.pustaka.data.library.scan.LibraryScans
import javax.inject.Inject

@HiltAndroidApp
class PustakaApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var storageAccess: StorageAccess
    @Inject lateinit var libraryScans: LibraryScans

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Scan every time the app is opened or comes back to the foreground. Without access, the
        // first-launch screens ask for it and start the first scan once it's granted.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (storageAccess.hasAccess()) libraryScans.requestScan()
            }
        })
    }
}
