package com.cameronbatten.dadmusic.di

import com.cameronbatten.dadmusic.data.db.SongDao
import com.cameronbatten.dadmusic.playback.PlaybackCommands
import com.cameronbatten.dadmusic.playback.PlaybackController
import com.cameronbatten.dadmusic.playback.PlaybackSession
import com.cameronbatten.dadmusic.playback.SongLookup
import com.cameronbatten.dadmusic.ui.library.SongDaoSource
import com.cameronbatten.dadmusic.ui.library.SongSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds narrow interfaces consumed by LibraryViewModel and PlayerViewModel to their
 * production implementations.
 *
 * - [SongSource]       → [SongDaoSource]      (wraps [SongDao])
 * - [PlaybackCommands] → [PlaybackController]  (narrow: play only; used by LibraryViewModel)
 * - [PlaybackSession]  → [PlaybackController]  (broad: state + controls; used by PlayerViewModel)
 * - [SongLookup]       → [SongDao]             (findById; used by PlayerViewModel)
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

    @Binds
    @Singleton
    abstract fun bindPlaybackSession(impl: PlaybackController): PlaybackSession

    @Binds
    abstract fun bindSongLookup(impl: SongDao): SongLookup
}
