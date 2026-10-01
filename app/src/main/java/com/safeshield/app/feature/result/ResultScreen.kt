package com.safeshield.app.feature.result

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeshield.app.R
import com.safeshield.app.core.Formatters
import com.safeshield.app.domain.model.Severity
import com.safeshield.app.domain.model.ThreatFinding
import com.safeshield.app.ui.components.ResultAdBanner
import com.safeshield.app.ui.components.SectionCard
import com.safeshield.app.ui.components.SeverityChip
import com.safeshield.app.ui.theme.NeutralGrey
import com.safeshield.app.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    scanId: Long,
    onDone: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val adsRemoved by viewModel.adsRemoved.collectAsStateWithLifecycle()
    val exported by viewModel.exportedPath.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(exported) {
        exported?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeExport()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.results_title)) },
                actions = {
                    TextButton(onClick = viewModel::exportReport) {
                        Text(stringResource(R.string.export_report))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val data = summary
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Stat(stringResource(R.string.items_scanned), "${data?.itemsScanned ?: 0}")
                        Stat(
                            stringResource(R.string.duration),
                            Formatters.duration(data?.durationMillis ?: 0L),
                        )
                        Stat(
                            stringResource(R.string.severity_critical),
                            "${data?.threatsFound ?: 0}",
                        )
                    }
                }
            }

            if (data != null && data.findings.isEmpty()) {
                item {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = SafeGreen)
                            Spacer(Modifier.height(0.dp))
                            Text(
                                "  " + stringResource(R.string.no_threats),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }

            items(data?.findings.orEmpty(), key = { it.targetId }) { finding ->
                ThreatCard(
                    finding = finding,
                    onUninstall = { viewModel.uninstall(finding) },
                    onWhitelist = { viewModel.whitelist(finding) },
                    onAppInfo = { viewModel.openAppInfo(finding) },
                    onDelete = { viewModel.deleteFile(finding) },
                )
            }

            item {
                // Single small banner, results screen only. Never during a scan.
                ResultAdBanner(adsRemoved = adsRemoved, modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                ) { Text(stringResource(R.string.ok)) }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = NeutralGrey)
    }
}

@Composable
private fun ThreatCard(
    finding: ThreatFinding,
    onUninstall: () -> Unit,
    onWhitelist: () -> Unit,
    onAppInfo: () -> Unit,
    onDelete: () -> Unit,
) {
    SectionCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                finding.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            SeverityChip(finding.severity, severityLabel(finding.severity))
        }
        finding.malwareName?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = NeutralGrey)
        }
        Spacer(Modifier.height(6.dp))
        finding.reasons.forEach { reason ->
            Text("• $reason", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (finding.packageName != null) {
                TextButton(onClick = onUninstall) { Text(stringResource(R.string.uninstall)) }
                TextButton(onClick = onAppInfo) { Text(stringResource(R.string.app_info)) }
            } else {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete_file)) }
            }
            TextButton(onClick = onWhitelist) { Text(stringResource(R.string.whitelist)) }
        }
    }
}

@Composable
private fun severityLabel(severity: Severity): String = stringResource(
    when (severity) {
        Severity.CRITICAL -> R.string.severity_critical
        Severity.HIGH -> R.string.severity_high
        Severity.MEDIUM -> R.string.severity_medium
        Severity.LOW -> R.string.severity_low
        Severity.CLEAN -> R.string.severity_clean
    }
)
