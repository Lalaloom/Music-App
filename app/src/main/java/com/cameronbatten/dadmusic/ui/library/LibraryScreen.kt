package com.cameronbatten.dadmusic.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.R
import com.cameronbatten.dadmusic.data.db.Song

private val ROW_MIN_HEIGHT = 72.dp

/**
 * Library screen — shows the user's full song list.
 *
 * ## Layout
 * - [Scaffold] with a [TopAppBar] showing "Your Library" and a folder icon that
 *   opens the folder picker via [onOpenFolders].
 * - **Empty state**: centred prompt + "Pick a folder" button → [onOpenFolders].
 * - **Song list**: [LazyColumn] with 72 dp rows; title + dimmed artist subtitle.
 *   Tapping a row calls [LibraryViewModel.onSongTapped] then [onSongPlaying].
 *
 * All user-visible strings are resolved from `res/values/strings.xml`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenFolders: () -> Unit,
    onSongPlaying: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = onOpenFolders) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.folders_icon_description),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (songs.isEmpty()) {
            EmptyLibrary(
                onOpenFolders = onOpenFolders,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                items(songs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        onClick = {
                            viewModel.onSongTapped(song)
                            onSongPlaying()
                        },
                    )
                }
            }
        }
    }
}

// ── Private sub-composables ──────────────────────────────────────────────────

@Composable
private fun EmptyLibrary(
    onOpenFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.library_empty_prompt),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Button(
                onClick = onOpenFolders,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .heightIn(min = 64.dp),
            ) {
                Text(stringResource(R.string.library_pick_folder_button))
            }
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .semantics(mergeDescendants = true) {}
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = song.artist ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}
