package com.cameronbatten.dadmusic.di

import com.cameronbatten.dadmusic.data.folder.FolderRegistry
import com.cameronbatten.dadmusic.data.folder.FolderUriSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds [FolderRegistry] as the application-wide [FolderUriSource] implementation.
 *
 * Consumers such as [com.cameronbatten.dadmusic.data.scan.FileScanner] depend on the narrower
 * [FolderUriSource] interface, which keeps [FolderRegistry] `final` while remaining testable.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FolderBindsModule {

    @Binds
    @Singleton
    abstract fun bindFolderUriSource(impl: FolderRegistry): FolderUriSource
}
