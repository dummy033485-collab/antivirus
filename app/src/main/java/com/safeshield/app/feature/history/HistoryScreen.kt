package com.safeshield.app.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.safeshield.app.ui.theme.DangerRed
import com.safeshield.app.ui.theme.NeutralGrey
import com.safeshield.app.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::clear) {
                        Text(stringResource(R.string.clear_history))
                    }
                },
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            EmptyState(stringResource(R.string.history_empty), Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(history, key = { it.id }) { record ->
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                record.scanType.name,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                Formatters.dateTime(record.finishedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralGrey,
                            )
                            Text(
                                stringResource(
                                    R.string.scanned_count,
                                    record.itemsScanned,
                                    record.itemsScanned,
                                ) + " · " + Formatters.duration(record.durationMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralGrey,
                            )
                        }
                        Text(
                            stringResource(R.string.threats_found, record.threatsFound),
                            color = if (record.threatsFound > 0) DangerRed else SafeGreen,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
