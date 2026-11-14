package my.naqibrafiqi.pustaka.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.naqibrafiqi.pustaka.data.library.access.AndroidStorageAccess
import my.naqibrafiqi.pustaka.data.library.access.StorageAccess
import my.naqibrafiqi.pustaka.data.library.epub.CoverStore
import my.naqibrafiqi.pustaka.data.library.epub.EpubReader
import my.naqibrafiqi.pustaka.data.library.epub.ReadiumEpubReader
import my.naqibrafiqi.pustaka.data.library.scan.LibraryScans
import my.naqibrafiqi.pustaka.data.library.scan.WorkManagerLibraryScans
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {

    @Binds
    abstract fun bindEpubReader(impl: ReadiumEpubReader): EpubReader

    @Binds
    abstract fun bindStorageAccess(impl: AndroidStorageAccess): StorageAccess

    @Binds
    abstract fun bindLibraryScans(impl: WorkManagerLibraryScans): LibraryScans

    companion object {

        @Provides
        @Singleton
        fun provideCoverStore(@ApplicationContext context: Context): CoverStore =
            CoverStore(File(context.cacheDir, "covers"))

        /** Set up by [my.naqibrafiqi.pustaka.PustakaApplication], which provides Hilt's worker factory. */
        @Provides
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
            WorkManager.getInstance(context)
    }
}
