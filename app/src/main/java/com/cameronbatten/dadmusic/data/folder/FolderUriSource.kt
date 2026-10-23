package com.cameronbatten.dadmusic.data.folder

import android.net.Uri

/**
 * Narrow interface over the set of folder [Uri]s that are currently resolvable (i.e. have an
 * active persistable read permission).
 *
 * This exists so that consumers like [com.cameronbatten.dadmusic.data.scan.FileScanner] can be
 * tested without subclassing or mocking the full [FolderRegistry] implementation.  Production
 * code is bound to [FolderRegistry] via Hilt; tests supply a trivial [FolderUriSource] fake.
 */
interface FolderUriSource {
    /**
     * Returns the currently resolvable folder [Uri]s.  Order is implementation-defined but
     * stable within a single call.
     */
    fun resolvableUris(): List<Uri>
}
