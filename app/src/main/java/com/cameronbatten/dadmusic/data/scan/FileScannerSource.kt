package com.cameronbatten.dadmusic.data.scan

/**
 * Narrow interface over [FileScanner] so that [LibraryImporter] can be tested with a fake
 * scanner that returns a hardcoded [List<DiscoveredFile>] without needing a real filesystem
 * or SAF permissions.
 *
 * Production code is bound to [FileScanner] via Hilt; tests supply a trivial fake.
 */
interface FileScannerSource {
    /**
     * Returns all discoverable music/video files. Order and deduplication semantics are
     * implementation-defined; see [FileScanner] for production behaviour.
     */
    suspend fun scanAll(): List<DiscoveredFile>
}
