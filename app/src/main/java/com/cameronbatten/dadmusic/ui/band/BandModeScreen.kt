package com.cameronbatten.dadmusic.ui.band

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cameronbatten.dadmusic.R
import com.cameronbatten.dadmusic.band.BandMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandModeScreen(
    onBack: () -> Unit,
    viewModel: BandModeViewModel = hiltViewModel(),
) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val peers by viewModel.peers.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.band_screen_title)) },
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
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.band_screen_blurb),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(
                            when (mode) {
                                BandMode.OFF -> R.string.band_mode_off
                                BandMode.HOSTING -> R.string.band_mode_hosting
                                BandMode.JOINED -> R.string.band_mode_joined
                            },
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    when (mode) {
                        BandMode.OFF -> {
                            Button(
                                onClick = viewModel::startHosting,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) { Text(stringResource(R.string.band_host_cta)) }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = viewModel::startJoining,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) { Text(stringResource(R.string.band_join_cta)) }
                        }
                        else -> {
                            OutlinedButton(
                                onClick = viewModel::stop,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) { Text(stringResource(R.string.band_stop_cta)) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            if (peers.isNotEmpty()) {
                Text(
                    stringResource(R.string.band_peers_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (p in peers) {
                        Text(
                            "${p.name} — ${p.host}:${p.port}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
