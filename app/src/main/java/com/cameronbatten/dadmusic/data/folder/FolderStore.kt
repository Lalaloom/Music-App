package com.cameronbatten.dadmusic.data.folder

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Narrow interface over the mutable, observable set of registered music folders.
 *
 * [FolderRegistry] is the production implementation, bound via Hilt.
 * Tests supply a lightweight fake without touching [android.content.SharedPreferences]
 * or [android.content.ContentResolver] permission APIs.
 */
interface FolderStore {
    /** Emits the current set of folder [Uri]s and re-emits on every change. */
    fun observe(): Flow<Set<Uri>>

    /** Registers [uri] as a watched music folder. */
    suspend fun add(uri: Uri)

    /** Un-registers [uri]. No-op if it was not present. */
    suspend fun remove(uri: Uri)
}
