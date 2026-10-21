package com.cameronbatten.dadmusic.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.cameronbatten.dadmusic.R
import com.cameronbatten.dadmusic.data.db.FileType

private val PLAY_PAUSE_SIZE = 64.dp
private val SKIP_SIZE = 48.dp

/**
 * Full-screen player that renders:
 *
 * - **Audio mode** ([FileType.AUDIO] or no song): centred title + artist, transport
 *   controls (Previous · Play/Pause · Next), scrubber [Slider].
 * - **Video mode** ([FileType.VIDEO]): full-screen [PlayerView] via [AndroidView] with
 *   auto-fading controls; back-button overlay at top-left.
 *
 * Error banner shown for non-null [PlaybackState.lastError].
 */
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    val isVideo = currentSong?.fileType == FileType.VIDEO

    if (isVideo) {
        VideoPlayerContent(
            onBack = onBack,
            lastError = state.lastError,
            viewModel = viewModel,
        )
    } else {
        AudioPlayerContent(
            onBack = onBack,
            title = currentSong?.title ?: stringResource(R.string.player_unknown_title),
            artist = currentSong?.artist ?: stringResource(R.string.player_unknown_artist),
            isPlaying = state.isPlaying,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            lastError = state.lastError,
            onPlayPause = { viewModel.playPause() },
            onNext = { viewModel.next() },
            onPrevious = { viewModel.previous() },
            onSeek = { ms -> viewModel.seek(ms) },
        )
    }
}

// ── Audio mode ────────────────────────────────────────────────────────────────

@Composable
private fun AudioPlayerContent(
    onBack: () -> Unit,
    title: String,
    artist: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    lastError: String?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    Scaffold(
        containerColor = Color.Black,
        contentColor = Color.White,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_description),
                    tint = Color.White,
                )
            }

            // Main content column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.testTag("player_title"),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Artist
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.testTag("player_artist"),
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Transport controls
                TransportControls(
                    isPlaying = isPlaying,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Scrubber
                Scrubber(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeek = onSeek,
                )
            }

            // Error banner
            if (lastError != null) {
                ErrorBanner(
                    message = lastError,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Previous — 48dp
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(SKIP_SIZE),
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = stringResource(R.string.player_skip_previous_description),
                modifier = Modifier.fillMaxSize(),
                tint = Color.White,
            )
        }

        // Play/Pause — 64dp primary
        Surface(
            onClick = onPlayPause,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(PLAY_PAUSE_SIZE)
                .testTag("player_play_pause"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.player_play_pause_description),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        // Next — 48dp
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(SKIP_SIZE),
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(R.string.player_skip_next_description),
                modifier = Modifier.fillMaxSize(),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun Scrubber(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    // Track slider drag locally; commit on release.
    // `isDragging` gates the `remember` key so position-polling (every 500ms) does
    // NOT snap the thumb back to the live position while the user is still dragging.
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
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player_scrubber"),
    )
}

@Composable
private fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .testTag("player_error_message"),
            )
        }
    }
}

// ── Video mode ────────────────────────────────────────────────────────────────

@Composable
private fun VideoPlayerContent(
    onBack: () -> Unit,
    lastError: String?,
    viewModel: PlayerViewModel,
) {
    // Capture the PlayerView so DisposableEffect can clear its surface listener on dispose.
    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            // Defensive: detach the player so Media3 can release surface listeners cleanly.
            playerViewRef.value?.player = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("player_video_view"),
    ) {
        // Media3 PlayerView fills the entire screen; controls auto-fade
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = true
                    // Attach the player; null-safe — attach again in update if needed
                    player = viewModel.getPlayer()
                    playerViewRef.value = this
                }
            },
            update = { playerView ->
                // Re-attach whenever composition updates (e.g. after config change)
                playerView.player = viewModel.getPlayer()
                playerViewRef.value = playerView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Back button overlay
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back_description),
                tint = Color.White,
            )
        }

        // Error banner — same component as audio mode for consistency
        if (lastError != null) {
            ErrorBanner(
                message = lastError,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}
