package com.cameronbatten.dadmusic.ui.setlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cameronbatten.dadmusic.R
import com.cameronbatten.dadmusic.data.db.Setlist
import com.cameronbatten.dadmusic.ui.navigator.SongInQueue
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Left pane of the navigator: setlist dropdown + drag-reorderable live queue +
 * "save changes" footer.
 */
@Composable
fun SetlistPane(
    setlists: List<Setlist>,
    activeSetlistId: Long?,
    queue: List<SongInQueue>,
    hasDiverged: Boolean,
    currentSongId: Long?,
    onSwitch: (Long?) -> Unit,
    onApplyOrder: (songIds: List<Long>) -> Unit,
    onRemove: (position: Int) -> Unit,
    onSaveToParent: () -> Unit,
    onRowTap: (position: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
        SetlistDropdown(
            setlists = setlists,
            activeSetlistId = activeSetlistId,
            onSelect = onSwitch,
        )

        Spacer(Modifier.height(12.dp))

        val currentIndex = queue.indexOfFirst { it.song.id == currentSongId }

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (activeSetlistId == null) R.string.navigator_queue_empty_library
                        else R.string.navigator_queue_empty_setlist,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            ReorderableQueueList(
                queue = queue,
                currentSongId = currentSongId,
                onApplyOrder = onApplyOrder,
                onRemove = onRemove,
                onRowTap = onRowTap,
                modifier = Modifier.weight(1f),
            )
        }

        if (hasDiverged && activeSetlistId != null) {
            val activeName = setlists.firstOrNull { it.id == activeSetlistId }?.name.orEmpty()
            Button(
                onClick = onSaveToParent,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(vertical = 8.dp),
            ) {
                Text(stringResource(R.string.navigator_save_changes, activeName))
            }
        }
    }
}

@Composable
private fun ReorderableQueueList(
    queue: List<SongInQueue>,
    currentSongId: Long?,
    onApplyOrder: (songIds: List<Long>) -> Unit,
    onRemove: (position: Int) -> Unit,
    onRowTap: (position: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current

    // Local mirror of the queue. The reorder lib calls onMove continuously
    // during a drag and expects the underlying list to update synchronously;
    // if we round-tripped every onMove through the DB the lib would see stale
    // positions mid-drag and the animation would jitter. Instead we mutate
    // this local list in onMove, then persist the final order on drag stop.
    var localQueue by remember { mutableStateOf(queue) }
    // When upstream emits a different queue (new song added / removed / parent
    // switched), resync — but ignore churn that comes from our own persisted
    // reorder (same ids in the same order).
    LaunchedEffect(queue) {
        if (queue.map { it.song.id } != localQueue.map { it.song.id }) {
            localQueue = queue
        } else {
            // Same ids, same order — upstream may still have refreshed Song
            // fields (title, status). Adopt the fresher objects.
            localQueue = queue
        }
    }

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localQueue = localQueue.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
    ) {
        items(localQueue, key = { it.song.id }) { inQueue ->
            ReorderableItem(reorderState, key = inQueue.song.id) { isDragging ->
                val index = localQueue.indexOf(inQueue)
                val currentIndex = localQueue.indexOfFirst { it.song.id == currentSongId }
                LiveQueueRow(
                    songInQueue = inQueue,
                    isCurrent = inQueue.song.id == currentSongId,
                    isPlayed = currentIndex >= 0 && index in 0 until currentIndex,
                    isDragging = isDragging,
                    dragHandleModifier = Modifier.longPressDraggableHandle(
                        onDragStarted = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragStopped = {
                            // Snapshot and persist the final order.
                            val ids = localQueue.map { it.song.id }
                            onApplyOrder(ids)
                        },
                    ),
                    onTap = { onRowTap(inQueue.entry.position) },
                    onRemove = { onRemove(inQueue.entry.position) },
                )
            }
        }
    }
}

@Composable
private fun SetlistDropdown(
    setlists: List<Setlist>,
    activeSetlistId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val activeName = setlists.firstOrNull { it.id == activeSetlistId }?.name
        ?: stringResource(R.string.navigator_library_option)

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(
                text = activeName,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.navigator_setlist_dropdown_cd),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.navigator_library_option)) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            setlists.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.name) },
                    onClick = {
                        onSelect(s.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
