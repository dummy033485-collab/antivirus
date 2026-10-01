package com.safeshield.app.feature.apps

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeshield.app.R
import com.safeshield.app.domain.model.AppRiskInfo
import com.safeshield.app.domain.model.RiskLevel
import com.safeshield.app.ui.components.EmptyState
import com.safeshield.app.ui.components.RiskChip
import com.safeshield.app.ui.components.SectionCard
import com.safeshield.app.ui.theme.NeutralGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: AppsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.apps_title)) }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.search_apps)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            FilterChip(
                selected = state.showSystem,
                onClick = viewModel::toggleSystemApps,
                label = { Text(stringResource(R.string.show_system_apps)) },
            )
            Spacer(Modifier.height(8.dp))

            when {
                state.loading -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                state.error != null -> EmptyState(stringResource(R.string.error_generic))

                state.visible.isEmpty() -> EmptyState(stringResource(R.string.history_empty))

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.visible, key = { it.packageName }) { app ->
                        AppRiskCard(
                            app = app,
                            onOpenInfo = { viewModel.openAppInfo(app) },
                            onUninstall = { viewModel.uninstall(app) },
                            onWhitelist = { viewModel.toggleWhitelist(app) },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AppRiskCard(
    app: AppRiskInfo,
    onOpenInfo: () -> Unit,
    onUninstall: () -> Unit,
    onWhitelist: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${app.versionName} · ${app.riskScore}/100",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralGrey,
                )
            }
            RiskChip(app.riskLevel, riskLabel(app.riskLevel))
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.why_risky),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (app.reasonResIds.isEmpty()) {
                    Text(
                        stringResource(R.string.risk_low),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeutralGrey,
                    )
                }
                app.reasonResIds.forEach { resId ->
                    Text("• " + stringResource(resId), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(onClick = onOpenInfo) { Text(stringResource(R.string.app_info)) }
                    if (!app.isSystemApp) {
                        TextButton(onClick = onUninstall) { Text(stringResource(R.string.uninstall)) }
                    }
                    TextButton(onClick = onWhitelist) {
                        Text(stringResource(R.string.whitelist))
                    }
                }
            }
        }
    }
}

@Composable
private fun riskLabel(level: RiskLevel): String = stringResource(
    when (level) {
        RiskLevel.HIGH -> R.string.risk_high
        RiskLevel.MEDIUM -> R.string.risk_medium
        RiskLevel.LOW -> R.string.risk_low
    }
)
