package com.safeshield.app.feature.junk

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeshield.app.R
import com.safeshield.app.core.Formatters
import com.safeshield.app.ui.components.EmptyState
import com.safeshield.app.ui.components.SectionCard
import com.safeshield.app.ui.theme.NeutralGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JunkScreen(onBack: () -> Unit, viewModel: JunkViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.junk_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            SectionCard {
                Text(
                    Formatters.bytes(state.totalBytes),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    stringResource(R.string.junk_cache_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeutralGrey,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = viewModel::cleanOwnJunk,
                        enabled = state.ownCleanableBytes > 0,
                    ) {
                        Text(
                            stringResource(
                                R.string.junk_clean,
                                Formatters.bytes(state.ownCleanableBytes),
                            )
                        )
                    }
                    TextButton(onClick = { viewModel.openStorageSettings(null) }) {
                        Text(stringResource(R.string.junk_open_storage_settings))
                    }
                }
                if (!state.hasUsageAccess) {
                    TextButton(onClick = viewModel::openUsageAccess) {
                        Text(stringResource(R.string.applock_grant_usage))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (state.items.isEmpty() && !state.loading) {
                EmptyState(stringResource(R.string.junk_empty))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.items, key = { it.label + it.sizeBytes }) { item ->
                        SectionCard(onClick = { viewModel.openStorageSettings(item.packageName) }) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(item.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    Formatters.bytes(item.sizeBytes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeutralGrey,
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
