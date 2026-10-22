package com.cameronbatten.dadmusic.data.scan

import android.net.Uri

/**
 * Represents a music or video file discovered during a [FileScanner] walk.
 *
 * @param uri         The [Uri] that uniquely identifies the file (SAF document URI or file URI).
 * @param displayName The file name as returned by [androidx.documentfile.provider.DocumentFile.getName],
 *                    e.g. `"Hotel California - Eagles.mp3"`.
 * @param extension   Lower-case extension without a leading dot, e.g. `"mp3"`.
 * @param mimeType    MIME type from [androidx.documentfile.provider.DocumentFile.getType], may be null.
 * @param sizeBytes   File size in bytes from [androidx.documentfile.provider.DocumentFile.length].
 */
data class DiscoveredFile(
    val uri: Uri,
    val displayName: String,
    val extension: String,
    val mimeType: String?,
    val sizeBytes: Long,
)
