package com.cameronbatten.dadmusic.lyrics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Atomic file-backed cache for fetched LRC content.
 *
 * Files live under `context.filesDir/lyrics/<songId>.lrc`. The internal filesDir
 * is scoped-storage safe — no SAF permission needed, deleted automatically if the
 * user uninstalls the app, and never written to the user's own folders. The user's
 * disk sidecars (see [com.cameronbatten.dadmusic.data.scan.LrcSidecarReader]) are
 * read-only; nothing in this class ever touches them.
 *
 * ### Atomicity
 * [write] writes to `<songId>.lrc.tmp` first, then renames onto the final path.
 * A crash during the write leaves at most a `.tmp` file (ignored by [read]) and
 * never a half-written final file. The rename falls back to `delete + rename` so
 * the same code works on filesystems whose `rename` doesn't overwrite.
 */
@Singleton
class LrcCacheStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val lyricsDir: File by lazy {
        File(context.filesDir, "lyrics").also { it.mkdirs() }
    }

    /**
     * Writes [lrcText] to the cache for [songId] and returns the absolute path of
     * the final file. Overwrites any prior content.
     */
    suspend fun write(songId: Long, lrcText: String): String = withContext(Dispatchers.IO) {
        val target = fileFor(songId)
        val tmp = File(lyricsDir, "$songId.lrc.tmp")
        tmp.writeText(lrcText, Charsets.UTF_8)
        if (!tmp.renameTo(target)) {
            target.delete()
            if (!tmp.renameTo(target)) {
                tmp.delete()
                throw IOException("LrcCacheStore: failed to rename $tmp onto $target")
            }
        }
        target.absolutePath
    }

    /** Returns the cached LRC text for [songId], or null if the file is absent. */
    suspend fun read(songId: Long): String? = withContext(Dispatchers.IO) {
        val f = fileFor(songId)
        if (f.exists()) f.readText(Charsets.UTF_8) else null
    }

    /** Deletes the cache file for [songId]; no-op when missing. */
    suspend fun delete(songId: Long): Boolean = withContext(Dispatchers.IO) {
        fileFor(songId).delete()
    }

    /** Returns the absolute path where [songId]'s cache file lives (whether or not it exists). */
    fun pathFor(songId: Long): String = fileFor(songId).absolutePath

    private fun fileFor(songId: Long) = File(lyricsDir, "$songId.lrc")
}
