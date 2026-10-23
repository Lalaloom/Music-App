package com.cameronbatten.dadmusic.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.R
import com.cameronbatten.dadmusic.data.db.Song

/**
 * Hidden admin / override panel (spec §5.12). Reachable only via long-press on
 * the Settings → About section header. Dangerous actions require confirmation;
 * resetting device state requires typing "RESET" into a text field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onResetComplete: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val entitlements by viewModel.entitlements.collectAsStateWithLifecycle()
    val override by viewModel.adminOverride.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val work by viewModel.work.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snack.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.resetComplete.collect { onResetComplete() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.admin_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_description),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            EntitlementSection(
                proEnabled = entitlements.proEnabled,
                override = override,
                onPick = viewModel::setOverride,
            )
            Spacer(Modifier.height(16.dp))
            ForceActionsSection(
                work = work,
                onRescan = viewModel::rescanNow,
                onClearLyrics = viewModel::clearLyricsCache,
                onRefetchLyrics = viewModel::refetchAllLyrics,
            )
            Spacer(Modifier.height(16.dp))
            SongEditorSection(songs = songs, onSave = viewModel::saveSongEdit)
            Spacer(Modifier.height(16.dp))
            DangerZoneSection(onReset = viewModel::resetDeviceState)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    isDanger: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors =
        if (isDanger) {
            androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        } else {
            androidx.compose.material3.CardDefaults.elevatedCardColors()
        }
    ElevatedCard(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun EntitlementSection(
    proEnabled: Boolean,
    override: Boolean?,
    onPick: (Boolean?) -> Unit,
) {
    SectionCard(stringResource(R.string.admin_section_entitlement)) {
        Text(
            text = stringResource(
                if (proEnabled) R.string.admin_entitlement_state_pro
                else R.string.admin_entitlement_state_free,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = override == null,
                onClick = { onPick(null) },
                label = { Text(stringResource(R.string.admin_entitlement_default)) },
                modifier = Modifier.testTag("admin_entitlement_default"),
            )
            FilterChip(
                selected = override == true,
                onClick = { onPick(true) },
                label = { Text(stringResource(R.string.admin_entitlement_force_pro)) },
                modifier = Modifier.testTag("admin_entitlement_force_pro"),
            )
            FilterChip(
                selected = override == false,
                onClick = { onPick(false) },
                label = { Text(stringResource(R.string.admin_entitlement_force_free)) },
                modifier = Modifier.testTag("admin_entitlement_force_free"),
            )
        }
    }
}

@Composable
private fun ForceActionsSection(
    work: AdminWork?,
    onRescan: () -> Unit,
    onClearLyrics: () -> Unit,
    onRefetchLyrics: () -> Unit,
) {
    var pendingAction by remember { mutableStateOf<AdminWork?>(null) }

    SectionCard(stringResource(R.string.admin_section_force)) {
        Button(
            onClick = onRescan,
            enabled = work == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.admin_force_rescan))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { pendingAction = AdminWork.ClearLyrics },
            enabled = work == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.admin_force_clear_lyrics))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { pendingAction = AdminWork.RefetchLyrics },
            enabled = work == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.admin_force_refetch))
        }
        work?.let {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (it) {
                    AdminWork.Rescan -> stringResource(R.string.admin_work_rescan)
                    AdminWork.ClearLyrics -> stringResource(R.string.admin_work_clear_lyrics)
                    AdminWork.RefetchLyrics -> stringResource(R.string.admin_work_refetch)
                    AdminWork.ResetDevice -> stringResource(R.string.admin_work_reset)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = {
                Text(
                    stringResource(
                        when (action) {
                            AdminWork.ClearLyrics -> R.string.admin_confirm_clear_title
                            AdminWork.RefetchLyrics -> R.string.admin_confirm_refetch_title
                            else -> R.string.admin_confirm_clear_title
                        },
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        when (action) {
                            AdminWork.ClearLyrics -> R.string.admin_confirm_clear_body
                            AdminWork.RefetchLyrics -> R.string.admin_confirm_refetch_body
                            else -> R.string.admin_confirm_clear_body
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingAction = null
                    when (action) {
                        AdminWork.ClearLyrics -> onClearLyrics()
                        AdminWork.RefetchLyrics -> onRefetchLyrics()
                        else -> Unit
                    }
                }) {
                    Text(stringResource(R.string.admin_confirm_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(stringResource(R.string.admin_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun SongEditorSection(
    songs: List<Song>,
    onSave: (Long, String, String?, Boolean) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedId by rememberSaveable { mutableStateOf(-1L) }

    SectionCard(stringResource(R.string.admin_section_songs)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.admin_songs_filter_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        val filtered = remember(songs, query) {
            if (query.isBlank()) songs
            else songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    (it.artist ?: "").contains(query, ignoreCase = true)
            }
        }
        if (filtered.isEmpty()) {
            Text(
                stringResource(R.string.admin_songs_empty),
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                items(filtered, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        expanded = song.id == expandedId,
                        onToggle = { expandedId = if (expandedId == song.id) -1L else song.id },
                        onSave = { title, artist, confirmed ->
                            onSave(song.id, title, artist, confirmed)
                            expandedId = -1L
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSave: (String, String?, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = song.artist ?: stringResource(R.string.admin_songs_artist_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onToggle) {
                Text(
                    stringResource(
                        if (expanded) R.string.admin_songs_close else R.string.admin_songs_edit,
                    ),
                )
            }
        }
        if (expanded) {
            var title by rememberSaveable(song.id) { mutableStateOf(song.title) }
            var artist by rememberSaveable(song.id) { mutableStateOf(song.artist ?: "") }
            var confirmed by rememberSaveable(song.id) { mutableStateOf(song.confirmed) }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.admin_songs_title_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text(stringResource(R.string.admin_songs_artist_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                Text(stringResource(R.string.admin_songs_confirmed_label))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = song.uri,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            val dirty = title.trim() != song.title ||
                (artist.trim().ifEmpty { null }) != song.artist ||
                confirmed != song.confirmed
            Button(
                onClick = { onSave(title, artist.ifBlank { null }, confirmed) },
                enabled = dirty && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.admin_songs_save))
            }
        }
    }
}

@Composable
private fun DangerZoneSection(onReset: () -> Unit) {
    var confirmStep by remember { mutableStateOf(0) }
    var typed by remember { mutableStateOf("") }

    SectionCard(stringResource(R.string.admin_section_danger), isDanger = true) {
        Text(
            stringResource(R.string.admin_reset_blurb),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { confirmStep = 1 },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth().testTag("admin_reset_button"),
        ) {
            Text(stringResource(R.string.admin_reset_cta))
        }
    }

    if (confirmStep == 1) {
        AlertDialog(
            onDismissRequest = { confirmStep = 0 },
            title = { Text(stringResource(R.string.admin_reset_confirm1_title)) },
            text = { Text(stringResource(R.string.admin_reset_confirm1_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmStep = 2
                    typed = ""
                }) {
                    Text(stringResource(R.string.admin_reset_confirm1_cta))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStep = 0 }) {
                    Text(stringResource(R.string.admin_confirm_cancel))
                }
            },
        )
    }

    if (confirmStep == 2) {
        AlertDialog(
            onDismissRequest = { confirmStep = 0 },
            title = { Text(stringResource(R.string.admin_reset_confirm2_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.admin_reset_confirm2_body))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_reset_type_box"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = typed.trim().equals("RESET", ignoreCase = false),
                    onClick = {
                        confirmStep = 0
                        onReset()
                    },
                ) {
                    Text(stringResource(R.string.admin_reset_confirm2_cta))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStep = 0 }) {
                    Text(stringResource(R.string.admin_confirm_cancel))
                }
            },
        )
    }
}
