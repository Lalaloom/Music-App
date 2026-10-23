package com.cameronbatten.dadmusic.di

import com.cameronbatten.dadmusic.lyrics.LyricsRepository
import com.cameronbatten.dadmusic.lyrics.SongLyricsFetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LyricsBindsModule {

    @Binds
    @Singleton
    abstract fun bindSongLyricsFetcher(impl: LyricsRepository): SongLyricsFetcher
}
