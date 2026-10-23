package com.cameronbatten.dadmusic.ui.pedal

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.R
import com.cameronbatten.dadmusic.pedal.PedalAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedalBindingsScreen(
    onBack: () -> Unit,
    viewModel: PedalBindingsViewModel = hiltViewModel(),
) {
    val bindings by viewModel.bindings.collectAsStateWithLifecycle()
    var captureTarget by remember { mutableStateOf<PedalAction?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.pedal_screen_title)) },
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
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.pedal_screen_blurb),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))

            for (action in PedalAction.entries) {
                val currentKeyCode = bindings.firstOrNull { it.action == action }?.keyCode
                PedalRow(
                    label = action.label,
                    keyCode = currentKeyCode,
                    onCapture = { captureTarget = action },
                    onClear = { viewModel.clear(action) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    captureTarget?.let { action ->
        CaptureDialog(
            action = action,
            onDismiss = { captureTarget = null },
            onCaptured = { code ->
                viewModel.bind(action, code)
                captureTarget = null
            },
        )
    }
}

@Composable
private fun PedalRow(
    label: String,
    keyCode: Int?,
    onCapture: () -> Unit,
    onClear: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (keyCode != null)
                        stringResource(R.string.pedal_row_bound, keyCode)
                    else stringResource(R.string.pedal_row_unbound),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (keyCode != null) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.pedal_row_clear))
                }
            }
            OutlinedButton(onClick = onCapture) {
                Text(stringResource(R.string.pedal_row_capture))
            }
        }
    }
}

@Composable
private fun CaptureDialog(
    action: PedalAction,
    onDismiss: () -> Unit,
    onCaptured: (Int) -> Unit,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(action) { requester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pedal_capture_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.pedal_capture_body, action.label),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                // A hidden focusable surface receives the next hardware KeyEvent.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .focusRequester(requester)
                        .focusable()
                        .onPreviewKeyEvent { ev ->
                            if (ev.type == KeyEventType.KeyDown) {
                                onCaptured(ev.key.nativeKeyCode)
                                true
                            } else false
                        },
                ) {}
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pedal_capture_cancel))
            }
        },
    )
}
