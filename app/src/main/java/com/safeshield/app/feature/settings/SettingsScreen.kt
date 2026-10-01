package com.safeshield.app.feature.settings

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeshield.app.BuildConfig
import com.safeshield.app.R
import com.safeshield.app.core.Formatters
import com.safeshield.app.data.prefs.AppLanguage
import com.safeshield.app.data.prefs.ScanSchedule
import com.safeshield.app.data.prefs.ThemeMode
import com.safeshield.app.ui.components.SectionCard
import com.safeshield.app.ui.theme.DangerRed
import com.safeshield.app.ui.theme.NeutralGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPrivacy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signatureCount by viewModel.signatureCount.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    val deletedText = stringResource(R.string.setting_data_deleted)
    LaunchedEffect(message) {
        if (message == "deleted") {
            snackbar.showSnackbar(deletedText)
            viewModel.consumeMessage()
        } else if (message != null) {
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionCard {
                Text(stringResource(R.string.setting_theme), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = {
                                Text(
                                    stringResource(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> R.string.theme_system
                                            ThemeMode.LIGHT -> R.string.theme_light
                                            ThemeMode.DARK -> R.string.theme_dark
                                        }
                                    )
                                )
                            },
                        )
                    }
                }
            }

            SectionCard {
                Text(stringResource(R.string.setting_language), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = state.language == lang,
                            onClick = { viewModel.setLanguage(lang) },
                            label = {
                                Text(
                                    stringResource(
                                        if (lang == AppLanguage.ENGLISH) R.string.language_english
                                        else R.string.language_urdu
                                    )
                                )
                            },
                        )
                    }
                }
            }

            SectionCard {
                ToggleRow(
                    title = stringResource(R.string.setting_online_mode),
                    subtitle = stringResource(R.string.setting_online_mode_desc),
                    checked = state.onlineModeEnabled,
                    onCheckedChange = viewModel::setOnlineMode,
                )
                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    title = stringResource(R.string.setting_realtime),
                    subtitle = stringResource(R.string.setting_realtime_desc),
                    checked = state.realtimeProtectionEnabled,
                    onCheckedChange = viewModel::setRealtime,
                )
            }

            SectionCard {
                Text(stringResource(R.string.setting_schedule), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanSchedule.entries.forEach { schedule ->
                        FilterChip(
                            selected = state.schedule == schedule,
                            onClick = { viewModel.setSchedule(schedule) },
                            label = {
                                Text(
                                    stringResource(
                                        when (schedule) {
                                            ScanSchedule.OFF -> R.string.schedule_off
                                            ScanSchedule.DAILY -> R.string.schedule_daily
                                            ScanSchedule.WEEKLY -> R.string.schedule_weekly
                                        }
                                    )
                                )
                            },
                        )
                    }
                }
            }

            SectionCard(onClick = viewModel::updateSignatures) {
                Text(stringResource(R.string.setting_update_db), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        R.string.setting_db_version,
                        signatureCount,
                        if (state.signatureUpdatedAt > 0) Formatters.dateTime(state.signatureUpdatedAt) else "—",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeutralGrey,
                )
            }

            SectionCard {
                Text(stringResource(R.string.setting_remove_ads), style = MaterialTheme.typography.titleMedium)
                if (state.adsRemoved) {
                    Text(stringResource(R.string.setting_ads_removed), color = NeutralGrey)
                } else {
                    Button(onClick = { (context as? Activity)?.let(viewModel::buyRemoveAds) }) {
                        Text(price ?: stringResource(R.string.setting_remove_ads))
                    }
                }
            }

            SectionCard(onClick = onOpenPrivacy) {
                Text(stringResource(R.string.setting_privacy), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.privacy_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeutralGrey,
                )
            }

            SectionCard {
                Button(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.setting_delete_data)) }
            }

            Text(
                stringResource(R.string.setting_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = NeutralGrey,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.setting_delete_data)) },
            text = { Text(stringResource(R.string.setting_delete_data_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAllData()
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.not_now))
                }
            },
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = NeutralGrey)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
