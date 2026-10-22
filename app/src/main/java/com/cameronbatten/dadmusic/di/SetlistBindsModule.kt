package com.cameronbatten.dadmusic.di

import com.cameronbatten.dadmusic.setlist.LiveQueueManager
import com.cameronbatten.dadmusic.setlist.QueueAppender
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SetlistBindsModule {

    @Binds
    @Singleton
    abstract fun bindQueueAppender(impl: LiveQueueManager): QueueAppender
}
