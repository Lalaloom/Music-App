package com.cameronbatten.dadmusic.di

import android.content.Context
import androidx.room.Room
import com.cameronbatten.dadmusic.data.db.AppDatabase
import com.cameronbatten.dadmusic.data.db.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application-level Hilt module.
 * Provides Room database and DAO singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    // TODO(M2): write a proper Migration for the sizeBytes column added in v2. For M1 we fall
    //           back to destructive migration — the app is pre-release and the only data is a
    //           song index that can be rebuilt in seconds via "Scan now".
    fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "dad_music.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSongDao(db: AppDatabase): SongDao = db.songDao()
}
