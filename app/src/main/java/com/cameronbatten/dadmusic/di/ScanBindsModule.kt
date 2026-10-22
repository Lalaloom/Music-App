package com.cameronbatten.dadmusic.di

import com.cameronbatten.dadmusic.data.scan.FileScanner
import com.cameronbatten.dadmusic.data.scan.FileScannerSource
import com.cameronbatten.dadmusic.data.scan.LibraryImporter
import com.cameronbatten.dadmusic.data.scan.LibraryImporterSource
import com.cameronbatten.dadmusic.data.scan.MetadataReader
import com.cameronbatten.dadmusic.data.scan.MetadataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds concrete scan implementations to their narrow interfaces.
 *
 * [LibraryImporter] depends on [FileScannerSource] and [MetadataSource] so that tests can
 * substitute lightweight fakes without a real filesystem or media files.
 * [FoldersViewModel] depends on [LibraryImporterSource] for the same reason.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScanBindsModule {

    @Binds
    @Singleton
    abstract fun bindFileScannerSource(impl: FileScanner): FileScannerSource

    @Binds
    @Singleton
    abstract fun bindMetadataSource(impl: MetadataReader): MetadataSource

    @Binds
    @Singleton
    abstract fun bindLibraryImporterSource(impl: LibraryImporter): LibraryImporterSource
}
