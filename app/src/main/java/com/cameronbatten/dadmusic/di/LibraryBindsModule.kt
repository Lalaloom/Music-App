package com.cameronbatten.dadmusic.di

import com.cameronbatten.dadmusic.data.db.SongDao
import com.cameronbatten.dadmusic.playback.PlaybackController
import com.cameronbatten.dadmusic.ui.library.PlaybackCommands
import com.cameronbatten.dadmusic.ui.library.SongSource
import com.cameronbatten.dadmusic.ui.library.SongDaoSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds narrow interfaces consumed by [LibraryViewModel] to their production implementations.
 *
 * - [SongSource] → [SongDaoSource] (wraps [SongDao])
 * - [PlaybackCommands] → [PlaybackController] (the same singleton used by PlayerViewModel)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryBindsModule {

    @Binds
    @Singleton
    abstract fun bindSongSource(impl: SongDaoSource): SongSource

    @Binds
    @Singleton
    abstract fun bindPlaybackCommands(impl: PlaybackController): PlaybackCommands
}
