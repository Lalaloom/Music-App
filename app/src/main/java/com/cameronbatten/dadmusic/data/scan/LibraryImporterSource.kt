package com.cameronbatten.dadmusic.data.scan

/**
 * Narrow interface over [LibraryImporter] so that consumers such as [FoldersViewModel] can be
 * tested with a lightweight fake that controls when [importAll] completes.
 *
 * Production code is bound to [LibraryImporter] via Hilt; tests supply a fake.
 */
interface LibraryImporterSource {
    /**
     * Scans all registered folders and upserts discovered files into the database.
     *
     * @return [ImportResult] with counts of added, updated, and skipped files.
     */
    suspend fun importAll(): ImportResult
}
