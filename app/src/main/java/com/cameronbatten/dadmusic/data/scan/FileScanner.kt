package com.cameronbatten.dadmusic.data.scan

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.cameronbatten.dadmusic.data.folder.FolderRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val ALLOWED_EXTENSIONS = setOf(
    "mp3", "m4a", "wav", "flac", "aac", "mp4", "mkv", "webm",
)

/**
 * Recursively walks every SAF folder URI registered in [FolderRegistry] and returns a flat
 * list of music/video [DiscoveredFile]s.
 *
 * ### Algorithm
 * For each resolvable URI:
 * 1. Wrap it in a [DocumentFile] tree root (or a file-URI root for tests).
 * 2. Iteratively expand children using an explicit [ArrayDeque] stack (avoids unbounded
 *    stack depth for deeply nested folder hierarchies).
 * 3. For each file whose extension is in [ALLOWED_EXTENSIONS] (case-insensitive), build
 *    a [DiscoveredFile] and add it to the accumulator.
 * 4. Deduplicate by URI string so the same physical file is never returned twice.
 *
 * ### Error handling
 * [SecurityException] and other exceptions for individual children are caught and swallowed;
 * one unreadable folder does not abort the whole scan.
 */
@Singleton
class FileScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderRegistry: FolderRegistry,
) {

    /**
     * Returns a deduplicated list of all [DiscoveredFile]s found under the registered folders.
     * Returns an empty list if no resolvable URIs are registered.
     *
     * Dispatches to [Dispatchers.IO]; safe to call from any coroutine context.
     */
    suspend fun scanAll(): List<DiscoveredFile> = withContext(Dispatchers.IO) {
        val roots = folderRegistry.resolvableUris()
        if (roots.isEmpty()) return@withContext emptyList()

        val seen = LinkedHashSet<String>()   // dedup by URI string
        val results = mutableListOf<DiscoveredFile>()

        for (rootUri in roots) {
            val rootDoc: DocumentFile? = openRoot(rootUri)
            if (rootDoc == null) continue
            walk(rootDoc, seen, results)
        }

        results
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Opens a [DocumentFile] for [uri].  Tries [DocumentFile.fromTreeUri] first (real SAF
     * tree URIs) and falls back to [DocumentFile.fromFile] for plain `file://` URIs that are
     * used by instrumented tests via [Uri.fromFile].
     */
    private fun openRoot(uri: Uri): DocumentFile? = try {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            DocumentFile.fromFile(File(path))
        } else {
            DocumentFile.fromTreeUri(context, uri)
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Iterative depth-first walk of [root].
     * Directories are pushed onto the stack; matching files are collected.
     */
    private fun walk(
        root: DocumentFile,
        seen: LinkedHashSet<String>,
        results: MutableList<DiscoveredFile>,
    ) {
        val stack = ArrayDeque<DocumentFile>()
        stack.addLast(root)

        while (stack.isNotEmpty()) {
            val current: DocumentFile = stack.removeLast()

            val children: Array<out DocumentFile> = try {
                current.listFiles()
            } catch (e: SecurityException) {
                // Permission revoked for this subtree — skip silently.
                continue
            } catch (e: Exception) {
                // I/O or other error — skip individual folder silently.
                continue
            }

            for (child in children) {
                if (child.isDirectory) {
                    stack.addLast(child)
                } else if (child.isFile) {
                    val uriKey = child.uri.toString()
                    if (uriKey in seen) continue
                    seen += uriKey

                    val fileName = child.name ?: continue
                    val ext = fileName.substringAfterLast('.', "").lowercase()
                    if (ext !in ALLOWED_EXTENSIONS) continue

                    results += DiscoveredFile(
                        uri = child.uri,
                        displayName = fileName,
                        extension = ext,
                        mimeType = child.type,
                        sizeBytes = child.length(),
                    )
                }
            }
        }
    }
}
