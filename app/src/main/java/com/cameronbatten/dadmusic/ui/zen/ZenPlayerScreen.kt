package com.cameronbatten.dadmusic.ui.zen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.R
import kotlinx.coroutines.delay

private const val CONTROLS_AUTO_HIDE_MS = 3_000L

/**
 * The Zen performance view: full-screen black, three centred lines of lyrics
 * (prev dimmed, current large/highlighted, next dimmed), auto-scrolling with
 * playback. Tap anywhere to reveal transport controls for 3 s.
 */
@Composable
fun ZenPlayerScreen(
    onBack: () -> Unit,
    onEditLyrics: (songId: Long) -> Unit = {},
    viewModel: ZenPlayerViewModel = hiltViewModel(),
) {
    val lines by viewModel.parsedLines.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentLineIndex.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var controlsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { controlsVisible = !controlsVisible })
            }
            .testTag("zen_root"),
    ) {
        LyricsStack(
            lines = lines.map { it.text },
            currentIndex = currentIndex,
            currentLineSp = fontSize.currentLineSp,
            neighbourLineSp = fontSize.neighbourLineSp,
        )

        if (lines.isEmpty()) {
            NoLyricsHint(
                songTitle = currentSong?.title ?: stringResource(R.string.player_unknown_title),
            )
        }

        BackChip(
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp),
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            TransportOverlay(
                isPlaying = playbackState.isPlaying,
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                canEdit = currentSong != null,
                onPlayPause = viewModel::playPause,
                onSkipNext = viewModel::skipNext,
                onSkipPrevious = viewModel::skipPrevious,
                onSeek = viewModel::seek,
                onEditLyrics = { currentSong?.id?.let(onEditLyrics) },
            )
        }
    }
}

@Composable
private fun LyricsStack(
    lines: List<String>,
    currentIndex: Int,
    currentLineSp: Int,
    neighbourLineSp: Int,
) {
    val prev = lines.getOrNull(currentIndex - 1).orEmpty()
    val cur = lines.getOrNull(currentIndex).orEmpty()
    val next = lines.getOrNull(currentIndex + 1).orEmpty()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = prev,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = neighbourLineSp.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("zen_prev"),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = cur,
            color = MaterialTheme.colorScheme.primary,
            fontSize = currentLineSp.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("zen_current"),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = next,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = neighbourLineSp.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("zen_next"),
        )
    }
}

@Composable
private fun NoLyricsHint(songTitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = songTitle,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.zen_no_lyrics_hint),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BackChip(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onBack,
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Black.copy(alpha = 0.45f),
        modifier = modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back_description),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun TransportOverlay(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    canEdit: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onEditLyrics: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(16.dp),
    ) {
        var isDragging by remember { mutableStateOf(false) }
        var dragValue by remember(if (isDragging) Unit else positionMs) {
            mutableFloatStateOf(positionMs.toFloat())
        }
        Slider(
            value = dragValue,
            onValueChange = {
                isDragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek(dragValue.toLong())
            },
            valueRange = 0f..maxOf(1f, durationMs.toFloat()),
            modifier = Modifier.fillMaxWidth().testTag("zen_scrubber"),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onSkipPrevious, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(R.string.player_skip_previous_description),
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Surface(
                onClick = onPlayPause,
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp).testTag("zen_play_pause"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.player_play_pause_description),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.player_skip_next_description),
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            IconButton(
                onClick = onEditLyrics,
                enabled = canEdit,
                modifier = Modifier.size(48.dp).testTag("zen_edit_lyrics"),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.zen_edit_lyrics_description),
                    tint = if (canEdit) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
