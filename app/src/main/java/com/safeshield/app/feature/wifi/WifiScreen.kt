package com.safeshield.app.feature.wifi

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.safeshield.app.ui.components.ScoreRing
import com.safeshield.app.ui.components.SectionCard
import com.safeshield.app.ui.theme.NeutralGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScreen(onBack: () -> Unit, viewModel: WifiViewModel = hiltViewModel()) {
    val info by viewModel.info.collectAsStateWithLifecycle()
    val needsLocation by viewModel.needsLocation.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wifi_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val data = info
            if (data == null || !data.connected) {
                Text(stringResource(R.string.wifi_not_connected))
            } else {
                Box(Modifier.fillMaxWidth(0.6f).aspectRatio(1f)) {
                    ScoreRing(data.rating, modifier = Modifier.fillMaxSize(), label = data.encryption)
                }
                Text(data.ssid ?: "—", style = MaterialTheme.typography.titleLarge)
                SectionCard {
                    Text(
                        when (data.encryption) {
                            "OPEN" -> stringResource(R.string.wifi_open_warning)
                            "WEP" -> stringResource(R.string.wifi_wep_warning)
                            else -> stringResource(R.string.wifi_secure)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            if (needsLocation) {
                SectionCard {
                    Text(
                        stringResource(R.string.wifi_location_needed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeutralGrey,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    ) { Text(stringResource(R.string.grant)) }
                }
            }

            OutlinedButton(onClick = viewModel::refresh) {
                Text(stringResource(R.string.wifi_rescan))
            }
        }
    }
}
