package com.cameronbatten.dadmusic.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.BuildConfig
import com.cameronbatten.dadmusic.R
import com.cameronbatten.dadmusic.preferences.EndBehaviour
import com.cameronbatten.dadmusic.preferences.FontSizePreset
import kotlin.math.roundToInt

private const val OFFSET_RANGE_MS = 2_000f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAdmin: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val offset by viewModel.lyricOffsetMs.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val fetchProgress by viewModel.fetchProgress.collectAsStateWithLifecycle()
    val endBehaviour by viewModel.onSongEnded.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_section_lyrics))
            LyricOffsetSlider(
                offsetMs = offset,
                onChange = viewModel::setLyricOffset,
            )

            Spacer(Modifier.height(24.dp))

            SectionHeader(stringResource(R.string.settings_section_font_size))
            FontSizeChips(
                current = fontSize,
                onPick = viewModel::setFontSize,
            )

            Spacer(Modifier.height(24.dp))

            SectionHeader(stringResource(R.string.settings_section_library))
            Button(
                onClick = viewModel::refetchAllLyrics,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.settings_refetch_button))
            }
            fetchProgress?.let { progress ->
                Spacer(Modifier.height(12.dp))
                if (progress.total > 0) {
                    LinearProgressIndicator(
                        progress = { progress.completed.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = stringResource(
                        R.string.settings_fetch_progress,
                        progress.completed,
                        progress.total,
                        progress.failed,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))

            SectionHeader(stringResource(R.string.settings_section_playback))
            EndBehaviourChips(
                current = endBehaviour,
                onPick = viewModel::setOnSongEnded,
            )

            Spacer(Modifier.height(24.dp))

            // About section header is also the hidden admin entry point:
            // long-press (≥ 500 ms) opens the admin / override screen. A short
            // tap does nothing visible — the screen is intentionally
            // undiscoverable for non-technical users (spec §5.12).
            Text(
                text = stringResource(R.string.settings_section_about),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onOpenAdmin,
                    )
                    .testTag("settings_about_header"),
            )
            Text(
                text = stringResource(R.string.settings_about_app, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_about_lyrics_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EndBehaviourChips(current: EndBehaviour, onPick: (EndBehaviour) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        EndBehaviour.entries.forEach { behaviour ->
            FilterChip(
                selected = behaviour == current,
                onClick = { onPick(behaviour) },
                label = {
                    Text(
                        text = when (behaviour) {
                            EndBehaviour.PAUSE -> stringResource(R.string.settings_end_pause)
                            EndBehaviour.AUTO_ADVANCE -> stringResource(R.string.settings_end_auto_advance)
                            EndBehaviour.REPEAT -> stringResource(R.string.settings_end_repeat)
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun LyricOffsetSlider(offsetMs: Long, onChange: (Long) -> Unit) {
    // Local drag mirror so slider feels responsive; commit on release.
    var isDragging by remember { androidx.compose.runtime.mutableStateOf(false) }
    var dragValue by remember(if (isDragging) Unit else offsetMs) {
        mutableFloatStateOf(offsetMs.toFloat())
    }
    Text(
        text = stringResource(R.string.settings_lyric_offset_label, dragValue.roundToInt()),
        style = MaterialTheme.typography.bodyMedium,
    )
    Slider(
        value = dragValue,
        onValueChange = {
            isDragging = true
            dragValue = it
        },
        onValueChangeFinished = {
            isDragging = false
            onChange(dragValue.toLong())
        },
        valueRange = -OFFSET_RANGE_MS..OFFSET_RANGE_MS,
        steps = 39, // 100 ms increments across -2000..+2000
    )
}

@Composable
private fun FontSizeChips(current: FontSizePreset, onPick: (FontSizePreset) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FontSizePreset.entries.forEach { preset ->
            FilterChip(
                selected = preset == current,
                onClick = { onPick(preset) },
                label = {
                    Text(
                        text = when (preset) {
                            FontSizePreset.SMALL -> stringResource(R.string.settings_font_small)
                            FontSizePreset.MEDIUM -> stringResource(R.string.settings_font_medium)
                            FontSizePreset.LARGE -> stringResource(R.string.settings_font_large)
                        },
                    )
                },
            )
        }
    }
}
