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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
 * - Tap the body → start playback.
 * - Move-up / move-down icons do a single-step reorder. We use icon buttons
 *   instead of a drag gesture for M2 — spec wants drag but a reliable Compose
 *   drag-to-reorder requires either a third-party dep or non-trivial custom
 *   logic that pushes M2 out. Buttons ship clean and a nicer drag UX can
 *   land in M3.
 * - Played rows (index < current-playing index) are dimmed + strikethrough
 *   per spec §5.5.
 */
@Composable
fun LiveQueueRow(
    songInQueue: SongInQueue,
    index: Int,
    total: Int,
    isCurrent: Boolean,
    isPlayed: Boolean,
    onTap: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val titleColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isPlayed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onBackground
    }
    val titleDecoration = if (isPlayed && !isCurrent) TextDecoration.LineThrough else TextDecoration.None

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onTap)
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
            tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
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
        IconButton(onClick = onMoveUp, enabled = index > 0) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(R.string.navigator_move_up_cd),
            )
        }
        IconButton(onClick = onMoveDown, enabled = index < total - 1) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.navigator_move_down_cd),
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.navigator_remove_from_queue_cd),
            )
        }
    }
}
