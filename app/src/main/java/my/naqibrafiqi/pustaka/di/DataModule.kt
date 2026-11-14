package my.naqibrafiqi.pustaka.di

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.naqibrafiqi.pustaka.data.local.PustakaDatabase
import my.naqibrafiqi.pustaka.data.repository.BookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultBookRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultHighlightRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultReadingRepository
import my.naqibrafiqi.pustaka.data.repository.DefaultSeriesRepository
import my.naqibrafiqi.pustaka.data.repository.HighlightRepository
import my.naqibrafiqi.pustaka.data.repository.ReadingRepository
import my.naqibrafiqi.pustaka.data.repository.SeriesRepository
import my.naqibrafiqi.pustaka.data.settings.DataStoreReaderSettingsRepository
import my.naqibrafiqi.pustaka.data.settings.DataStoreSettingsRepository
import my.naqibrafiqi.pustaka.data.settings.DefaultLibraryFolder
import my.naqibrafiqi.pustaka.data.settings.ReaderSettingsRepository
import my.naqibrafiqi.pustaka.data.settings.SettingsRepository
import java.io.File
import java.time.Clock
import my.naqibrafiqi.pustaka.data.export.CacheExportFiles
import my.naqibrafiqi.pustaka.data.export.ExportFiles
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PustakaDatabase =
        Room.databaseBuilder(context, PustakaDatabase::class.java, PustakaDatabase.NAME).build()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    /** `/storage/emulated/0/Pustaka` on most phones. Read with "All files access" from Phase 2. */
    @Provides
    @DefaultLibraryFolder
    @Suppress("DEPRECATION") // The direct path is what "All files access" is for.
    fun provideDefaultLibraryFolder(): String =
        File(Environment.getExternalStorageDirectory(), "Pustaka").path

    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSeriesRepository(impl: DefaultSeriesRepository): SeriesRepository

    @Binds
    @Singleton
    abstract fun bindBookRepository(impl: DefaultBookRepository): BookRepository

    @Binds
    @Singleton
    abstract fun bindReadingRepository(impl: DefaultReadingRepository): ReadingRepository

    @Binds
    @Singleton
    abstract fun bindHighlightRepository(impl: DefaultHighlightRepository): HighlightRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindReaderSettingsRepository(impl: DataStoreReaderSettingsRepository): ReaderSettingsRepository

    @Binds
    abstract fun bindExportFiles(impl: CacheExportFiles): ExportFiles
}
