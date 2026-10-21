package com.cameronbatten.dadmusic.data.folder

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "folder_registry"
private const val KEY_URIS = "uris"

/**
 * Persists SAF (Storage Access Framework) folder URIs across app restarts.
 *
 * On [add], the URI's persistable read permission is taken via
 * [android.content.ContentResolver.takePersistableUriPermission] and the URI string is stored
 * in a [SharedPreferences] set.  On [remove], the permission is released and the URI is removed.
 *
 * [resolvableUris] cross-checks stored URIs against
 * [android.content.ContentResolver.persistedUriPermissions] so that URIs whose permissions
 * were revoked externally (e.g. after a reboot without the permission being re-granted) are
 * silently filtered out.
 *
 * [observe] exposes the current set as a cold [Flow] that re-emits whenever the
 * [SharedPreferences] backing store changes.
 *
 * Hilt injectable at [SingletonComponent] scope — declare as a constructor-injected dependency;
 * no explicit [@Provides][dagger.Provides] entry is required in the Hilt module.
 */
@Singleton
class FolderRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
) : FolderUriSource {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Guards the read-modify-write cycle around [prefs] in [add] and [remove].
     *
     * [SharedPreferences.putStringSet] is a blind overwrite (no CAS), so two concurrent
     * `add`/`remove` calls can clobber each other.  Since the public API is `suspend`,
     * coroutine concurrency is part of the stated contract — we serialise the critical
     * section with a [Mutex].
     */
    private val mutex = Mutex()

    // ── Stored URIs helpers ───────────────────────────────────────────────────

    private fun storedUriStrings(): Set<String> =
        prefs.getStringSet(KEY_URIS, emptySet()) ?: emptySet()

    private fun storedUris(): Set<Uri> =
        storedUriStrings().mapTo(LinkedHashSet()) { Uri.parse(it) }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Grants a persistable read permission for [uri] and stores it in the registry.
     *
     * Safe to call if [uri] is already present — [SharedPreferences] deduplicates strings and
     * [ContentResolver.takePersistableUriPermission] is idempotent.
     */
    suspend fun add(uri: Uri) {
        // Take the permission BEFORE acquiring the lock so permission errors propagate
        // without blocking concurrent remove()s.
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        mutex.withLock {
            val updated = storedUriStrings().toMutableSet().apply { add(uri.toString()) }
            prefs.edit().putStringSet(KEY_URIS, updated).apply()
        }
    }

    /**
     * Releases the persistable read permission for [uri] and removes it from the registry.
     *
     * If [uri] is not present, this is a no-op.
     */
    suspend fun remove(uri: Uri) {
        // Release the permission BEFORE acquiring the lock so permission errors propagate
        // without blocking concurrent add()s.
        try {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Permission was already gone — continue with removal from prefs.
        }
        mutex.withLock {
            val updated = storedUriStrings().toMutableSet().apply { remove(uri.toString()) }
            prefs.edit().putStringSet(KEY_URIS, updated).apply()
        }
    }

    /**
     * Returns a [Flow] that emits the current set of stored [Uri]s and re-emits on every change.
     *
     * Uses a [SharedPreferences.OnSharedPreferenceChangeListener] backed by [callbackFlow].
     */
    fun observe(): Flow<Set<Uri>> = callbackFlow {
        // Emit the current snapshot immediately.
        trySend(storedUris())

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_URIS) {
                trySend(storedUris())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)

        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /**
     * Returns only those stored [Uri]s that still have an active persistable read permission
     * in [android.content.ContentResolver.persistedUriPermissions].
     *
     * URIs whose permissions were revoked externally (e.g. by the system after a factory-reset
     * of external storage) are silently excluded.
     */
    override fun resolvableUris(): List<Uri> {
        val activePermissions = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .mapTo(HashSet()) { it.uri }
        return storedUris().filter { it in activePermissions }
    }
}
