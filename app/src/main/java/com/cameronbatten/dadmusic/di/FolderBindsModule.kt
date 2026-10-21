package com.cameronbatten.dadmusic.di

import com.cameronbatten.dadmusic.data.folder.FolderRegistry
import com.cameronbatten.dadmusic.data.folder.FolderStore
import com.cameronbatten.dadmusic.data.folder.FolderUriSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds [FolderRegistry] as the application-wide [FolderUriSource] and [FolderStore]
 * implementations.
 *
 * Consumers such as [com.cameronbatten.dadmusic.data.scan.FileScanner] depend on the narrower
 * [FolderUriSource] interface, which keeps [FolderRegistry] `final` while remaining testable.
 * [FolderStore] is the narrower interface consumed by [FoldersViewModel].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FolderBindsModule {

    @Binds
    @Singleton
    abstract fun bindFolderUriSource(impl: FolderRegistry): FolderUriSource

    @Binds
    @Singleton
    abstract fun bindFolderStore(impl: FolderRegistry): FolderStore
}
