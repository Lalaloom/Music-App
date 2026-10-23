package com.cameronbatten.dadmusic.ui.lyricseditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedLyricsEditorScreen(
    onBack: () -> Unit,
    viewModel: AdvancedLyricsEditorViewModel = hiltViewModel(),
) {
    val song by viewModel.song.collectAsStateWithLifecycle()
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.positionMs.collectAsStateWithLifecycle()
    val duration by viewModel.durationMs.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(song?.title ?: stringResource(R.string.lyrics_editor_title_fallback))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_description),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch { viewModel.save(); onBack() }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.lyrics_editor_save_cd),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TimelineBar(
                positionMs = position,
                durationMs = duration,
                isPlaying = isPlaying,
                onPlayPause = viewModel::playPause,
                onRewindAndPlay = viewModel::rewind2sAndPlay,
                onSeek = viewModel::seekTo,
            )
            Spacer(Modifier.height(4.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            ) {
                items(lines.size, key = { it }) { idx ->
                    val line = lines[idx]
                    LineRow(
                        index = idx,
                        text = line.text,
                        timestampMs = line.timestampMs,
                        onTextChange = { viewModel.setLineText(idx, it) },
                        onStamp = { viewModel.stampLineAtCurrentPosition(idx) },
                        onClearStamp = { viewModel.clearLineStamp(idx) },
                        onInsertBelow = { viewModel.insertLineAfter(idx) },
                        onRemove = { viewModel.removeLine(idx) },
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun TimelineBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRewindAndPlay: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Fake "waveform" strip: a horizontal line with tick marks every
            // 5 s and a playhead. Real amplitude-based waveform rendering is
            // deferred — this strip still gives the user a visual anchor
            // while timing lines.
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            ) {
                val w = size.width
                val h = size.height
                drawLine(
                    color = Color.Gray,
                    start = androidx.compose.ui.geometry.Offset(0f, h / 2),
                    end = androidx.compose.ui.geometry.Offset(w, h / 2),
                    strokeWidth = 1.5f,
                )
                if (durationMs > 0L) {
                    val tickCount = (durationMs / 5_000L).coerceAtMost(60).toInt()
                    for (i in 0..tickCount) {
                        val x = w * (i * 5_000L) / durationMs
                        drawLine(
                            color = Color.Gray,
                            start = androidx.compose.ui.geometry.Offset(x.toFloat(), h / 2 - 6f),
                            end = androidx.compose.ui.geometry.Offset(x.toFloat(), h / 2 + 6f),
                            strokeWidth = 1f,
                        )
                    }
                    val playheadX = (w * positionMs / durationMs).toFloat()
                    drawLine(
                        color = Color(0xFF4CAF50),
                        start = androidx.compose.ui.geometry.Offset(playheadX, 0f),
                        end = androidx.compose.ui.geometry.Offset(playheadX, h),
                        strokeWidth = 2.5f,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = positionMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onRewindAndPlay) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = stringResource(R.string.lyrics_editor_rewind_cd),
                    )
                }
                IconButton(onClick = onPlayPause, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.player_play_pause_description),
                    )
                }
                Text(
                    text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun LineRow(
    index: Int,
    text: String,
    timestampMs: Long?,
    onTextChange: (String) -> Unit,
    onStamp: () -> Unit,
    onClearStamp: () -> Unit,
    onInsertBelow: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.width(72.dp)) {
            IconButton(onClick = onStamp) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = stringResource(R.string.lyrics_editor_stamp_cd),
                )
            }
            Text(
                text = timestampMs?.let { formatTime(it) } ?: "—:—",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        IconButton(onClick = onInsertBelow) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.lyrics_editor_insert_cd),
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.lyrics_editor_remove_cd),
            )
        }
    }
    if (timestampMs != null) {
        Row(modifier = Modifier.padding(start = 80.dp)) {
            androidx.compose.material3.TextButton(onClick = onClearStamp) {
                Text(stringResource(R.string.lyrics_editor_clear_stamp))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val m = safe / 60_000
    val s = (safe % 60_000) / 1_000
    val cs = (safe % 1_000) / 10
    return "%02d:%02d.%02d".format(m, s, cs)
}
