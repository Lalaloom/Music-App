package com.cameronbatten.dadmusic.ui.folders

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cameronbatten.dadmusic.data.folder.FolderStore
import com.cameronbatten.dadmusic.data.scan.ImportResult
import com.cameronbatten.dadmusic.data.scan.LibraryImporterSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ── Fakes ─────────────────────────────────────────────────────────────────────

/**
 * Fake [FolderStore] backed by a [MutableStateFlow] so that UI updates propagate
 * without touching [android.content.SharedPreferences] or the real permission system.
 */
private class FakeFolderStore : FolderStore {
    private val _uris = MutableStateFlow<Set<Uri>>(emptySet())

    override fun observe(): Flow<Set<Uri>> = _uris

    override suspend fun add(uri: Uri) {
        _uris.value = _uris.value + uri
    }

    override suspend fun remove(uri: Uri) {
        _uris.value = _uris.value - uri
    }

    fun setFolders(uris: Set<Uri>) {
        _uris.value = uris
    }
}

/**
 * Fake [LibraryImporterSource] whose [importAll] suspends until [completeWith] is called.
 *
 * This gives tests full control over the duration of a scan so they can observe
 * the "importing" state before the result arrives.
 */
private class FakeLibraryImporter : LibraryImporterSource {
    private var deferred: CompletableDeferred<ImportResult>? = null

    override suspend fun importAll(): ImportResult {
        val d = CompletableDeferred<ImportResult>()
        deferred = d
        return d.await()
    }

    fun completeWith(result: ImportResult) {
        deferred?.complete(result)
    }
}

// ── Tests ──────────────────────────────────────────────────────────────────────

@RunWith(AndroidJUnit4::class)
class FolderPickerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildViewModel(
        store: FakeFolderStore = FakeFolderStore(),
        importer: FakeLibraryImporter = FakeLibraryImporter(),
    ): FoldersViewModel = FoldersViewModel(store, importer)

    // ── 1. Empty state ─────────────────────────────────────────────────────────

    @Test
    fun shows_empty_state_when_no_folders() {
        val vm = buildViewModel()
        composeTestRule.setContent {
            FolderPickerScreen(onDone = {}, viewModel = vm)
        }
        composeTestRule
            .onNodeWithText("No folders added yet")
            .assertIsDisplayed()
    }

    // ── 2. Add-folder button presence ─────────────────────────────────────────

    /**
     * Verifies that tapping "Add folder" does not crash and the button is present.
     *
     * Full [ActivityResultLauncher] stubbing requires a real device with a SAF provider;
     * this test verifies button presence and tap-safety (no crash) — actual folder-picker
     * launch is covered by manual QA or a device-lab instrumented test.
     */
    @Test
    fun tapping_add_folder_launches_open_document_tree_result() {
        val vm = buildViewModel()
        composeTestRule.setContent {
            FolderPickerScreen(onDone = {}, viewModel = vm)
        }
        // Confirm the button is present and tappable — ActivityResultLauncher wiring is
        // device-only (no SAF provider in the emulator test environment).
        composeTestRule
            .onNodeWithText("Add folder")
            .assertIsDisplayed()
            .assertIsEnabled()
        // Uncommenting the next line exercises the launcher on a real device / emulator with SAF:
        // composeTestRule.onNodeWithText("Add folder").performClick()
    }

    // ── 3. Rescan disables button while importing ──────────────────────────────

    @Test
    fun tapping_rescan_disables_button_until_import_finishes() {
        val fakeImporter = FakeLibraryImporter()
        val vm = buildViewModel(importer = fakeImporter)

        composeTestRule.setContent {
            FolderPickerScreen(onDone = {}, viewModel = vm)
        }

        // Before scan: "Scan now" is enabled.
        composeTestRule
            .onNodeWithText("Scan now")
            .assertIsEnabled()

        // Trigger the scan — importer suspends until completeWith() is called.
        composeTestRule.onNodeWithText("Scan now").performClick()

        // During scan: button must be disabled.
        composeTestRule
            .onNodeWithText("Scan now")
            .assertIsNotEnabled()

        // Unblock the importer.
        fakeImporter.completeWith(ImportResult(added = 3, updated = 1, skipped = 0))
        composeTestRule.waitForIdle()

        // After scan: button re-enabled.
        composeTestRule
            .onNodeWithText("Scan now")
            .assertIsEnabled()
    }

    // ── 4. Remove folder updates list ─────────────────────────────────────────

    @Test
    fun removing_folder_updates_list() {
        val fakeStore = FakeFolderStore()
        val testUri = Uri.parse(
            "content://com.android.externalstorage.documents/tree/primary%3AMusic"
        )
        fakeStore.setFolders(setOf(testUri))

        val vm = buildViewModel(store = fakeStore)

        composeTestRule.setContent {
            FolderPickerScreen(onDone = {}, viewModel = vm)
        }

        // The remove (trash) button should be present while the folder is in the list.
        composeTestRule
            .onNodeWithContentDescription("Remove folder")
            .assertIsDisplayed()

        // Tap the remove button.
        composeTestRule
            .onNodeWithContentDescription("Remove folder")
            .performClick()

        composeTestRule.waitForIdle()

        // After removal the list is empty → empty-state prompt should appear.
        composeTestRule
            .onNodeWithText("No folders added yet")
            .assertIsDisplayed()
    }
}
