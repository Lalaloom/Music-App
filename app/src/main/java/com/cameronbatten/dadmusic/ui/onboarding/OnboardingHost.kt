package com.cameronbatten.dadmusic.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.R

/**
 * 4-step onboarding wizard shown on first launch. Resumes always at step 1
 * if the app is killed mid-flow (per spec simplicity rule) — only `complete()`
 * flips the "done" preference.
 */
@Composable
fun OnboardingHost(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val scanStage by viewModel.scanStage.collectAsStateWithLifecycle()

    when (step) {
        OnboardingStep.WELCOME -> WelcomeScreen(onNext = viewModel::advanceToFolders)
        OnboardingStep.FOLDERS -> FoldersScreen(
            folders = folders,
            onAddFolder = viewModel::addFolder,
            onRemoveFolder = viewModel::removeFolder,
            onNext = viewModel::advanceToScanning,
        )
        OnboardingStep.SCANNING -> ScanningScreen(stage = scanStage)
        OnboardingStep.DONE -> DoneScreen(
            stage = scanStage,
            onOpenLibrary = { viewModel.complete(onFinish) },
        )
    }
}

@Composable
private fun WelcomeScreen(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.heightIn(min = 64.dp),
        ) {
            Text(stringResource(R.string.onboarding_welcome_cta))
        }
    }
}

@Composable
private fun FoldersScreen(
    folders: Set<Uri>,
    onAddFolder: (Uri) -> Unit,
    onRemoveFolder: (Uri) -> Unit,
    onNext: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onAddFolder)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_folders_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_folders_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        if (folders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.folders_empty_prompt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(folders.toList(), key = { it.toString() }) { uri ->
                    ListItem(
                        headlineContent = { Text(uri.lastPathSegment ?: uri.toString()) },
                        trailingContent = {
                            IconButton(
                                onClick = { onRemoveFolder(uri) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.folders_remove_folder_description),
                                )
                            }
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { launcher.launch(null) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.onboarding_folders_add))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(
                text = if (folders.isEmpty()) {
                    stringResource(R.string.onboarding_folders_skip)
                } else {
                    stringResource(R.string.onboarding_folders_next)
                },
            )
        }
    }
}

@Composable
private fun ScanningScreen(stage: ScanStage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_scanning_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        when (val s = stage) {
            is ScanStage.Idle, is ScanStage.Scanning -> {
                Text(
                    text = stringResource(R.string.onboarding_scanning_phase_scanning),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is ScanStage.Fetching -> {
                Text(
                    text = stringResource(
                        R.string.onboarding_scanning_phase_fetching,
                        s.progress.completed,
                        s.progress.total,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (s.progress.total > 0) {
                    LinearProgressIndicator(
                        progress = { s.progress.completed.toFloat() / s.progress.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            is ScanStage.Finished -> Unit // step advances; this screen fades out
        }
    }
}

@Composable
private fun DoneScreen(stage: ScanStage, onOpenLibrary: () -> Unit) {
    val result = (stage as? ScanStage.Finished)?.result

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_done_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (result != null && result.added > 0) {
                stringResource(R.string.onboarding_done_summary, result.added)
            } else {
                stringResource(R.string.onboarding_done_summary_empty)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onOpenLibrary,
            modifier = Modifier.heightIn(min = 64.dp),
        ) {
            Text(stringResource(R.string.onboarding_done_cta))
        }
    }
}
