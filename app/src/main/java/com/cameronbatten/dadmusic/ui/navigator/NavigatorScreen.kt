package com.cameronbatten.dadmusic.ui.navigator

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.R
import com.cameronbatten.dadmusic.ui.common.MiniPlayer
import com.cameronbatten.dadmusic.ui.search.SearchPane
import com.cameronbatten.dadmusic.ui.setlist.SetlistPane

/**
 * Primary screen for M2+. Replaces [com.cameronbatten.dadmusic.ui.library.LibraryScreen]
 * as the launch destination.
 *
 * Layout decision: `maxWidth >= 600 dp` lands on the Xiaomi tablet in landscape
 * (and most tablets in either orientation) → split pane. Phones and narrow
 * portrait → single pane with a tab toggle between Setlist and Library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigatorScreen(
    onOpenFolders: () -> Unit,
    onSongPlaying: () -> Unit,
    viewModel: NavigatorViewModel = hiltViewModel(),
) {
    val setlists by viewModel.setlists.collectAsStateWithLifecycle()
    val activeSetlistId by viewModel.activeSetlistId.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val hasDiverged by viewModel.hasDiverged.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.library_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenFolders) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.folders_icon_description),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            currentSong?.let { song ->
                MiniPlayer(
                    song = song,
                    isPlaying = playbackState.isPlaying,
                    onBodyClick = onSongPlaying,
                    onPlayPause = viewModel::playPause,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            val isWide = maxWidth >= 600.dp
            if (isWide) {
                Row(Modifier.fillMaxSize()) {
                    SetlistPane(
                        setlists = setlists,
                        activeSetlistId = activeSetlistId,
                        queue = queue,
                        hasDiverged = hasDiverged,
                        currentSongId = currentSong?.id,
                        onSwitch = viewModel::switchTo,
                        onApplyOrder = viewModel::applyQueueOrder,
                        onRemove = viewModel::removeFromQueue,
                        onSaveToParent = viewModel::saveQueueToParent,
                        onRowTap = { position ->
                            viewModel.playSongAt(position)
                            onSongPlaying()
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    HorizontalDivider(
                        modifier = Modifier.width(1.dp).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    SearchPane(modifier = Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                var showingSetlist by remember { mutableStateOf(true) }
                Column(Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = if (showingSetlist) 0 else 1) {
                        Tab(
                            selected = showingSetlist,
                            onClick = { showingSetlist = true },
                            text = { Text(stringResource(R.string.navigator_tab_setlist)) },
                        )
                        Tab(
                            selected = !showingSetlist,
                            onClick = { showingSetlist = false },
                            text = { Text(stringResource(R.string.navigator_tab_library)) },
                        )
                    }
                    if (showingSetlist) {
                        SetlistPane(
                            setlists = setlists,
                            activeSetlistId = activeSetlistId,
                            queue = queue,
                            hasDiverged = hasDiverged,
                            currentSongId = currentSong?.id,
                            onSwitch = viewModel::switchTo,
                            onApplyOrder = viewModel::applyQueueOrder,
                            onRemove = viewModel::removeFromQueue,
                            onSaveToParent = viewModel::saveQueueToParent,
                            onRowTap = { position ->
                                viewModel.playSongAt(position)
                                onSongPlaying()
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        SearchPane(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
