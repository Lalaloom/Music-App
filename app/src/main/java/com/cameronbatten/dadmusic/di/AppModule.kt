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
    // TODO(M2): add a Migration strategy (addMigrations / fallbackToDestructiveMigration) before
    //           bumping the Room schema version; omitting migrations on a version bump will crash
    //           on devices that have the previous version installed.
    fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "dad_music.db").build()

    @Provides
    fun provideSongDao(db: AppDatabase): SongDao = db.songDao()
}
