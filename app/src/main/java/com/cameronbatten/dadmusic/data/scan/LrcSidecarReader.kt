package com.cameronbatten.dadmusic.data.scan

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads a `.lrc` sidecar file's text content via [android.content.ContentResolver].
 *
 * Any exception while opening or decoding the stream maps to a `null` return so a
 * single unreadable sidecar never aborts the import pipeline. The file is read as
 * UTF-8; LRC files are text by convention and virtually all distribution tooling
 * emits UTF-8.
 */
@Singleton
class LrcSidecarReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : SidecarLyricsSource {

    override suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
        } catch (e: Exception) {
            null
        }
    }
}
