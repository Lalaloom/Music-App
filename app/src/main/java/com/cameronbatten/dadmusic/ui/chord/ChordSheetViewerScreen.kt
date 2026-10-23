package com.cameronbatten.dadmusic.ui.chord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.R
import com.cameronbatten.dadmusic.chord.ChordLine
import com.cameronbatten.dadmusic.chord.Transposer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordSheetViewerScreen(
    onBack: () -> Unit,
    viewModel: ChordSheetViewerViewModel = hiltViewModel(),
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val document by viewModel.document.collectAsStateWithLifecycle()
    val transpose by viewModel.transpose.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text(
                            title ?: stringResource(R.string.chord_viewer_fallback_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        artist?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
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
                    IconButton(onClick = viewModel::transposeDown) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.chord_viewer_transpose_down_cd),
                        )
                    }
                    Text(
                        text = formatTranspose(transpose),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    IconButton(onClick = viewModel::transposeUp) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.chord_viewer_transpose_up_cd),
                        )
                    }
                    IconButton(onClick = viewModel::resetTranspose) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.chord_viewer_transpose_reset_cd),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val doc = document
        if (doc == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.chord_viewer_loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                items(doc.lines.size, key = { it }) { idx ->
                    when (val line = doc.lines[idx]) {
                        is ChordLine.Blank -> Spacer(Modifier.height(12.dp))
                        is ChordLine.Comment -> Text(
                            text = line.text,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        is ChordLine.Content -> ContentLine(line, transpose)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentLine(line: ChordLine.Content, transpose: Int) {
    // Interleaved rendering: a chord row where chords sit directly above their
    // syllable, then the lyric row. Monospace keeps columns aligned.
    val chordAnnotated = AnnotatedString.Builder().apply {
        for (seg in line.segments) {
            val chord = seg.chord
            if (chord != null) {
                val shifted = Transposer.transpose(chord, transpose)
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.Unspecified,
                    ),
                ) {
                    append(shifted)
                }
                // Pad with spaces equal to the lyric length so the next chord
                // lines up over its syllable.
                val padCount = (seg.lyrics.length - shifted.length).coerceAtLeast(1)
                append(" ".repeat(padCount))
            } else {
                append(" ".repeat(seg.lyrics.length))
            }
        }
    }.toAnnotatedString()
    val lyricText = line.segments.joinToString(separator = "") { it.lyrics }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = chordAnnotated,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = lyricText,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

private fun formatTranspose(n: Int): String = when {
    n == 0 -> "±0"
    n > 0 -> "+$n"
    else -> "$n"
}
