package com.safeshield.app.feature.home

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeshield.app.R
import com.safeshield.app.core.Formatters
import com.safeshield.app.domain.model.ScanType
import com.safeshield.app.ui.components.ScoreRing
import com.safeshield.app.ui.components.SectionCard
import com.safeshield.app.ui.components.ToolRow
import com.safeshield.app.ui.theme.NeutralGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScanStarted: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showOptions by viewModel.showScanOptions.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    // Android 13+ needs explicit consent before we can show threat alerts.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.refreshRiskCounts()
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.startScan(ScanType.CUSTOM, uri)
            onScanStarted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(
                if (state.onlineMode) R.string.online_mode_badge else R.string.offline_mode_badge
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralGrey,
        )

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth(0.72f).aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            ScoreRing(
                score = state.securityScore,
                label = stringResource(R.string.security_score),
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(
                if (state.activeThreats == 0) R.string.protected_status else R.string.at_risk_status
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = state.lastScanAt?.let {
                stringResource(R.string.last_scan, Formatters.dateTime(it))
            } ?: stringResource(R.string.never_scanned),
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralGrey,
        )

        Spacer(Modifier.height(20.dp))

        PulsingScanButton(onClick = viewModel::openScanOptions)

        Spacer(Modifier.height(20.dp))

        SectionCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat(state.activeThreats.toString(), stringResource(R.string.threats_found, state.activeThreats).substringAfter(' '))
                Stat(state.highRiskApps.toString(), stringResource(R.string.risk_high))
            }
        }

        Spacer(Modifier.height(12.dp))

        ToolRow(
            icon = Icons.Filled.Warning,
            title = stringResource(R.string.apps_title),
            subtitle = stringResource(R.string.risk_high) + ": ${state.highRiskApps}",
            onClick = onOpenApps,
        )
        Spacer(Modifier.height(10.dp))
        ToolRow(
            icon = Icons.Filled.History,
            title = stringResource(R.string.history_title),
            onClick = onOpenHistory,
        )
        Spacer(Modifier.height(28.dp))
    }

    if (showOptions) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissScanOptions,
            sheetState = sheetState,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                ScanOption(
                    title = stringResource(R.string.quick_scan),
                    desc = stringResource(R.string.quick_scan_desc),
                ) {
                    viewModel.startScan(ScanType.QUICK); onScanStarted()
                }
                Spacer(Modifier.height(10.dp))
                ScanOption(
                    title = stringResource(R.string.full_scan),
                    desc = stringResource(R.string.full_scan_desc),
                ) {
                    viewModel.startScan(ScanType.FULL); onScanStarted()
                }
                Spacer(Modifier.height(10.dp))
                ScanOption(
                    title = stringResource(R.string.custom_scan),
                    desc = stringResource(R.string.custom_scan_desc),
                ) {
                    viewModel.dismissScanOptions()
                    folderPicker.launch(null)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = NeutralGrey)
    }
}

@Composable
private fun ScanOption(title: String, desc: String, onClick: () -> Unit) {
    SectionCard(onClick = onClick) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = NeutralGrey)
    }
}

@Composable
private fun PulsingScanButton(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulseScale",
    )
    Button(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth(0.8f)
            .height(60.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.scan_now), style = MaterialTheme.typography.titleMedium)
    }
}
