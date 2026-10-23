package com.cameronbatten.dadmusic.ui.navigator

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.R
import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.ui.common.MiniPlayer
import com.cameronbatten.dadmusic.ui.search.AddToQueueOutcome
import com.cameronbatten.dadmusic.ui.search.SearchPane
import com.cameronbatten.dadmusic.ui.setlist.SetlistPane
import kotlinx.coroutines.launch

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
    onOpenSettings: () -> Unit,
    onOpenPlayer: (FileType) -> Unit,
    onOpenChordSheets: () -> Unit = {},
    onOpenPdfSheets: () -> Unit = {},
    viewModel: NavigatorViewModel = hiltViewModel(),
) {
    val setlists by viewModel.setlists.collectAsStateWithLifecycle()
    val activeSetlistId by viewModel.activeSetlistId.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val hasDiverged by viewModel.hasDiverged.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.search_undo)
    val alreadyLabel = stringResource(R.string.search_already_present_dismiss)

    val onAddResult: (AddToQueueOutcome) -> Unit = handler@{ outcome ->
        snackbarScope.launch {
            // Dismiss any pending snackbar so a rapid second add doesn't queue
            // behind the first — the Undo button always targets the latest add.
            snackbarHostState.currentSnackbarData?.dismiss()
            when (outcome) {
                is AddToQueueOutcome.Added -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "“${outcome.title}” added to queue",
                        actionLabel = undoLabel,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long, // 10 s per spec §5.7
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.removeFromQueue(outcome.position)
                    }
                }
                is AddToQueueOutcome.AlreadyPresent -> {
                    snackbarHostState.showSnackbar(
                        message = "“${outcome.title}” is already in your queue",
                        actionLabel = alreadyLabel,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

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
                    IconButton(onClick = onOpenChordSheets) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = stringResource(R.string.chord_sheets_open_cd),
                        )
                    }
                    IconButton(onClick = onOpenPdfSheets) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = stringResource(R.string.pdf_sheets_open_cd),
                        )
                    }
                    IconButton(onClick = onOpenFolders) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.folders_icon_description),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_open_cd),
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
                    onBodyClick = { onOpenPlayer(song.fileType) },
                    onPlayPause = viewModel::playPause,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        onRowTap = { position, fileType ->
                            viewModel.playSongAt(position)
                            onOpenPlayer(fileType)
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    HorizontalDivider(
                        modifier = Modifier.width(1.dp).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    SearchPane(
                        onAddResult = onAddResult,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
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
                            onRowTap = { position, fileType ->
                                viewModel.playSongAt(position)
                                onOpenPlayer(fileType)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        SearchPane(
                            onAddResult = onAddResult,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
