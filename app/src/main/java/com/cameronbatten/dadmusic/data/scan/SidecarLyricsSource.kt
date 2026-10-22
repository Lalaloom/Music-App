package com.cameronbatten.dadmusic.data.scan

import android.net.Uri

/**
 * Narrow interface for reading a `.lrc` sidecar file's content.
 *
 * Separated from the concrete [LrcSidecarReader] so [LibraryImporter] can be driven
 * by fakes in instrumented tests without a real ContentResolver.
 */
interface SidecarLyricsSource {
    /** Returns the sidecar's text content, or null when the URI can't be read. */
    suspend fun read(uri: Uri): String?
}
