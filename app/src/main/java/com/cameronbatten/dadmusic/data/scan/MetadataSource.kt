package com.cameronbatten.dadmusic.data.scan

import android.net.Uri

/**
 * Narrow interface over [MetadataReader] so that [LibraryImporter] can be tested with a fake
 * reader that returns hardcoded [FileMetadata] values without needing real media files.
 *
 * Production code is bound to [MetadataReader] via Hilt; tests supply a trivial fake.
 */
interface MetadataSource {
    /**
     * Reads audio/video metadata from [uri].
     * Implementations must never throw — failures are surfaced as `FileMetadata` with null fields.
     */
    suspend fun read(uri: Uri): FileMetadata
}
