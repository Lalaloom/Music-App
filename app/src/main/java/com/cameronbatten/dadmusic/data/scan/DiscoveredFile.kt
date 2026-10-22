package com.cameronbatten.dadmusic.data.scan

import android.net.Uri

/**
 * Represents a music or video file discovered during a [FileScanner] walk.
 *
 * @param uri            Unique SAF document URI (or `file://` URI in tests) for the file.
 * @param displayName    File name as returned by [androidx.documentfile.provider.DocumentFile.getName],
 *                       e.g. `"Hotel California - Eagles.mp3"`.
 * @param extension      Lower-case extension without the leading dot, e.g. `"mp3"`.
 * @param mimeType       MIME type from [androidx.documentfile.provider.DocumentFile.getType], may be null.
 * @param sizeBytes      File size in bytes.
 * @param lrcSidecarUri  URI of a `<basename>.lrc` file sitting in the same directory as this
 *                       music file, or null when no sidecar exists. The [LibraryImporter]
 *                       adopts this file's content as the song's lyrics (spec §5.1) and
 *                       skips any LRCLIB fetch for that song.
 * @param rootFolderUri  String form of the registered root folder's URI this file was
 *                       reached from. Drives the folder → setlist bridge (spec §5.1): one
 *                       Setlist per registered folder.
 * @param rootFolderName Display name of that root folder (last SAF segment or
 *                       `DocumentFile.name`). Becomes the Setlist's name on first scan;
 *                       updates on subsequent scans when the user renames the folder.
 */
data class DiscoveredFile(
    val uri: Uri,
    val displayName: String,
    val extension: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val lrcSidecarUri: Uri? = null,
    val rootFolderUri: String? = null,
    val rootFolderName: String? = null,
)
