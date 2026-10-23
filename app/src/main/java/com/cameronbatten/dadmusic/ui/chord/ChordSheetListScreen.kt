package com.cameronbatten.dadmusic.ui.chord

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordSheetListScreen(
    onBack: () -> Unit,
    onOpenSheet: (Long) -> Unit,
    viewModel: ChordSheetListViewModel = hiltViewModel(),
) {
    val sheets by viewModel.sheets.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.chord_list_title)) },
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
        if (sheets.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
            ) {
                Text(
                    text = stringResource(R.string.chord_list_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                items(sheets, key = { it.id }) { sheet ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSheet(sheet.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(sheet.title, style = MaterialTheme.typography.bodyLarge)
                        sheet.artist?.let {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
