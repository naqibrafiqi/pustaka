package my.naqibrafiqi.pustaka.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.naqibrafiqi.pustaka.data.library.epub.CoverStore
import my.naqibrafiqi.pustaka.data.library.epub.EpubReader
import my.naqibrafiqi.pustaka.data.library.epub.ReadiumEpubReader
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {

    @Binds
    abstract fun bindEpubReader(impl: ReadiumEpubReader): EpubReader

    companion object {

        @Provides
        @Singleton
        fun provideCoverStore(@ApplicationContext context: Context): CoverStore =
            CoverStore(File(context.cacheDir, "covers"))
    }
}
