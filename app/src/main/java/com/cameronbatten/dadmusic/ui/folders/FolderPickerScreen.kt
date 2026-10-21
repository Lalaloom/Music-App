package com.cameronbatten.dadmusic.ui.folders

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cameronbatten.dadmusic.R

/**
 * Screen that lets the user manage their registered music folders and trigger a library rescan.
 *
 * ## Layout
 * - [Scaffold] with a [TopAppBar] showing "Your music folders".
 * - [LazyColumn] of folder rows; each row has:
 *   - The folder's last path segment as the visible label.
 *   - A Trash [IconButton] (min 64 dp tap target) that calls [FoldersViewModel.removeFolder].
 * - "Add folder" [Button] (≥ 64 dp) — opens the system [ActivityResultContracts.OpenDocumentTree]
 *   picker; on a non-null result, calls [FoldersViewModel.addFolder].
 * - "Scan now" [Button] (≥ 64 dp) — calls [FoldersViewModel.rescan]; disabled while importing.
 * - [LinearProgressIndicator] visible while [FoldersViewModel.isImporting] is `true`.
 * - Import result summary shown below the progress bar.
 * - Empty state when no folders are registered.
 *
 * All user-visible strings are defined in `res/values/strings.xml` under `R.string.*`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerScreen(
    onDone: () -> Unit,
    viewModel: FoldersViewModel = hiltViewModel(),
) {
    val folders by viewModel.folders().collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importResult by viewModel.importResult.collectAsState()

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { viewModel.addFolder(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.folders_title)) },
                actions = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.folders_done_button),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            // ── Progress + result ─────────────────────────────────────────────
            if (isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.folders_scanning))
            }

            importResult?.let { result ->
                if (!isImporting) {
                    Text(
                        text = stringResource(
                            R.string.folders_import_summary,
                            result.added,
                            result.updated,
                            result.skipped,
                        ),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Folder list or empty state ────────────────────────────────────
            if (folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.folders_empty_prompt))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(folders, key = { it.toString() }) { uri ->
                        FolderRow(
                            uri = uri,
                            onRemove = { viewModel.removeFolder(uri) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Button(
                onClick = { openDocumentTreeLauncher.launch(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Text(stringResource(R.string.folders_add_button))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.rescan() },
                enabled = !isImporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Text(stringResource(R.string.folders_scan_button))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Private sub-composables ──────────────────────────────────────────────────

@Composable
private fun FolderRow(
    uri: Uri,
    onRemove: () -> Unit,
) {
    val label = uri.lastPathSegment ?: uri.toString()

    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            // Minimum 64 dp tap target for the primary remove action.
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.folders_remove_folder_description),
                )
            }
        },
    )
}
