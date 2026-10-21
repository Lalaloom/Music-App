package com.cameronbatten.dadmusic.data.scan

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads ID3 / mp4 audio metadata from a [Uri] using [MediaMetadataRetriever].
 *
 * ### Contract
 * - Always dispatches to [Dispatchers.IO].
 * - On **any** [Throwable] (corrupt file, missing tags, permission error, codec failure),
 *   returns `FileMetadata(null, null, null, 0L)` — never throws to the caller.
 * - The retriever is always [MediaMetadataRetriever.release]d in `finally`.
 *
 * ### Review Focus #3 — crash-resilient fallback
 * Both tagless files and corrupt/unreadable files are handled identically: the `runCatching`
 * around [MediaMetadataRetriever.setDataSource] (and each key extraction) ensures that a bad
 * file cannot propagate an exception up the scan pipeline.
 */
@Singleton
class MetadataReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Reads audio metadata from [uri].
     *
     * Returns a [FileMetadata] populated with whatever the decoder can extract.
     * If the file is corrupt, unreadable, or tagless, returns `FileMetadata(null, null, null, 0L)`.
     * Never throws.
     */
    suspend fun read(uri: Uri): FileMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            runCatching { retriever.setDataSource(context, uri) }
                .onFailure {
                    // File is unreadable / corrupt — return safe fallback immediately.
                    return@withContext FileMetadata(null, null, null, 0L)
                }

            val title = retriever.safeExtract(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.safeExtract(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.safeExtract(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationMs = retriever
                .safeExtract(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L

            FileMetadata(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
            )
        } catch (t: Throwable) {
            // Defensive catch-all: any unexpected failure returns the safe fallback.
            FileMetadata(null, null, null, 0L)
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Extracts a metadata [key] from the retriever without throwing. Returns `null` on failure. */
    private fun MediaMetadataRetriever.safeExtract(key: Int): String? =
        runCatching { extractMetadata(key) }.getOrNull()
}

/**
 * Represents the audio metadata extracted from a single media file.
 *
 * All string fields are `null` when the file contains no embedded tag for that field.
 * [durationMs] is `0L` when the duration cannot be determined (corrupt file or read failure).
 */
data class FileMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long,  // 0L on failure
)
