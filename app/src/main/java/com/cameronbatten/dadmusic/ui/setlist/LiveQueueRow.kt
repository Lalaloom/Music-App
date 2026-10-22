package com.cameronbatten.dadmusic.ui.setlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cameronbatten.dadmusic.R
import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.ui.navigator.SongInQueue

/**
 * One row in the live queue.
 *
 * The row is deliberately spare — only a leading type icon, title + artist,
 * a drag handle, and a trash button — so the title has room to breathe in a
 * split-pane left column. Reorder is driven by a long-press drag on the
 * trailing drag handle (see [SetlistPane]); the handle slot takes
 * [dragHandleModifier] from the caller so this composable stays reorder-lib-
 * agnostic.
 *
 * - Tap the body (anywhere outside the drag handle / trash button) → play.
 * - Played rows (before the current one) dim + strikethrough per spec §5.5.
 */
@Composable
fun LiveQueueRow(
    songInQueue: SongInQueue,
    isCurrent: Boolean,
    isPlayed: Boolean,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onTap: () -> Unit,
    onRemove: () -> Unit,
) {
    val titleColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isPlayed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onBackground
    }
    val titleDecoration =
        if (isPlayed && !isCurrent) TextDecoration.LineThrough else TextDecoration.None

    val rowSurface = if (isDragging) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.background
    }

    Surface(
        color = rowSurface,
        tonalElevation = if (isDragging) 4.dp else 0.dp,
        shadowElevation = if (isDragging) 6.dp else 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(onClick = onTap)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val leadingIcon = when {
                isCurrent -> Icons.Default.PlayArrow
                songInQueue.song.fileType == FileType.VIDEO -> Icons.Default.Videocam
                else -> Icons.Default.MusicNote
            }
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = songInQueue.song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = titleColor,
                    textDecoration = titleDecoration,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = songInQueue.song.artist.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = titleDecoration,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.navigator_remove_from_queue_cd),
                )
            }
            // Drag handle — the caller attaches the lib's longPressDraggableHandle
            // modifier so this file stays reorder-lib-agnostic.
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.navigator_drag_handle_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = dragHandleModifier
                    .padding(end = 4.dp)
                    .size(32.dp),
            )
        }
    }
}
