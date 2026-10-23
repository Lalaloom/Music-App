package com.cameronbatten.dadmusic.di

import android.content.Context
import androidx.room.Room
import com.cameronbatten.dadmusic.data.db.ALL_MIGRATIONS
import com.cameronbatten.dadmusic.data.db.AppDatabase
import com.cameronbatten.dadmusic.data.db.LiveQueueDao
import com.cameronbatten.dadmusic.data.db.LyricsDao
import com.cameronbatten.dadmusic.data.db.SetlistDao
import com.cameronbatten.dadmusic.data.db.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Qualifier for a process-wide [CoroutineScope] that outlives any ViewModel. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Application-level Hilt module.
 * Provides Room database and DAO singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "dad_music.db")
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideSongDao(db: AppDatabase): SongDao = db.songDao()

    @Provides
    fun provideLyricsDao(db: AppDatabase): LyricsDao = db.lyricsDao()

    @Provides
    fun provideSetlistDao(db: AppDatabase): SetlistDao = db.setlistDao()

    @Provides
    fun provideLiveQueueDao(db: AppDatabase): LiveQueueDao = db.liveQueueDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
