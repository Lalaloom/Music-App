package com.cameronbatten.dadmusic.ui.lyricseditor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.R
import kotlinx.coroutines.launch

/**
 * Per-song lyric editor. User pastes / edits raw LRC; Save writes a
 * `source = USER_EDIT` [com.cameronbatten.dadmusic.data.db.Lyrics] row that
 * subsequent scans will not overwrite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditorScreen(
    onBack: () -> Unit,
    onOpenAdvanced: () -> Unit = {},
    viewModel: LyricsEditorViewModel = hiltViewModel(),
) {
    val song by viewModel.song.collectAsStateWithLifecycle()
    val existing by viewModel.existingLyrics.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Seed the text field whenever upstream (DAO) emits a new current lyrics row.
    var text by remember(existing?.rawLrc) {
        mutableStateOf(existing?.rawLrc ?: "")
    }

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
                    IconButton(onClick = onOpenAdvanced) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.lyrics_editor_advanced_cd),
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.save(text)
                            onBack()
                        }
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(12.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.lyrics_editor_placeholder)) },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                minLines = 12,
                modifier = Modifier.fillMaxWidth().fillMaxSize(),
            )
        }
    }
}
