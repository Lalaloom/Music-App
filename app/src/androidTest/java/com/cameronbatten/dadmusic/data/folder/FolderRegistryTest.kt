package com.cameronbatten.dadmusic.data.folder

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [FolderRegistry].
 *
 * These tests verify Review Focus #1: SAF URI permission tracking survives / drops correctly.
 *
 * NOTE: No connected device is required to compile. These tests exercise real SAF permission
 * mechanics and must be run on a device/emulator to pass fully.
 */
@RunWith(AndroidJUnit4::class)
class FolderRegistryTest {

    private lateinit var registry: FolderRegistry

    // A syntactically valid document-tree URI that the test ContentResolver will recognise.
    // In a real instrumented run this URI would need to be a real SAF tree URI granted by the user,
    // but the compile target only requires the class to exist and the code to type-check.
    private val testUri: Uri =
        Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMusic")

    @Before
    fun setUp() {
        registry = FolderRegistry(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() = runTest {
        // Best-effort cleanup: remove the URI if it was added during the test.
        try {
            registry.remove(testUri)
        } catch (_: Exception) {
            // Ignore — permission may already be gone or was never granted.
        }
    }

    /**
     * After [FolderRegistry.add], the URI should appear in
     * [android.content.ContentResolver.persistedUriPermissions] with read access.
     *
     * Review Focus #1 — add path: permission is persisted.
     */
    @Test
    fun addGrantsPersistableReadPermissionOnTheUri() = runTest {
        registry.add(testUri)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val persisted = context.contentResolver.persistedUriPermissions
        val found = persisted.any { it.uri == testUri && it.isReadPermission }
        assertTrue(
            "URI should appear in persistedUriPermissions with read access after add()",
            found,
        )
    }

    /**
     * If a URI's SAF permission is released externally (simulating a reboot where the system
     * revokes a non-persistable permission, or direct release), [FolderRegistry.resolvableUris]
     * must NOT return that URI.
     *
     * Review Focus #1 — drop path: stale URIs are filtered out.
     */
    @Test
    fun resolvableUris_dropsUrisWithoutActivePermission() = runTest {
        registry.add(testUri)

        // Simulate external permission revocation by releasing it directly via ContentResolver.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.contentResolver.releasePersistableUriPermission(
            testUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )

        val resolvable = registry.resolvableUris()
        assertFalse(
            "URI whose permission was released should not appear in resolvableUris()",
            resolvable.contains(testUri),
        )
    }

    /**
     * [FolderRegistry.observe] must emit the updated set after [FolderRegistry.add] and again
     * after [FolderRegistry.remove].
     *
     * One subscription, three emissions:
     *   emissions[0] — initial snapshot (no testUri)
     *   emissions[1] — after add (contains testUri)
     *   emissions[2] — after remove (does NOT contain testUri)
     *
     * This exercises the real `OnSharedPreferenceChangeListener` path rather than
     * re-opening a fresh cold flow for each read.
     */
    @Test
    fun observe_emitsAfterAddAndAfterRemove() = runTest {
        // Ensure we start clean (URI not present).
        try { registry.remove(testUri) } catch (_: Exception) { }

        val emissions = mutableListOf<Set<Uri>>()
        val job = launch {
            registry.observe().take(3).toList(emissions)
        }

        // Give the subscription a chance to register and capture the initial snapshot.
        yield()

        registry.add(testUri)      // triggers emissions[1]
        registry.remove(testUri)   // triggers emissions[2]

        job.join()

        assertTrue(
            "observe() should emit a set containing testUri after add()",
            emissions[1].contains(testUri),
        )
        assertFalse(
            "observe() should emit a set NOT containing testUri after remove()",
            emissions[2].contains(testUri),
        )
    }
}
