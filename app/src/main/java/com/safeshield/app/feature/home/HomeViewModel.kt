package com.safeshield.app.feature.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.repository.AppInventoryRepository
import com.safeshield.app.data.repository.ScanRepository
import com.safeshield.app.domain.engine.SecurityScoreCalculator
import com.safeshield.app.domain.model.RiskLevel
import com.safeshield.app.domain.model.ScanType
import com.safeshield.app.domain.model.Severity
import com.safeshield.app.service.ScanController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val securityScore: Int = 100,
    val activeThreats: Int = 0,
    val highRiskApps: Int = 0,
    val lastScanAt: Long? = null,
    val onlineMode: Boolean = false,
    val signatureCount: Int = 0,
    val loading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scanController: ScanController,
    private val scanRepository: ScanRepository,
    private val inventory: AppInventoryRepository,
    private val settings: SettingsRepository,
    private val scoreCalculator: SecurityScoreCalculator,
) : ViewModel() {

    private val riskCounts = MutableStateFlow(0 to 0) // high to medium

    val uiState: StateFlow<HomeUiState> = combine(
        scanRepository.observeActiveThreats(),
        scanRepository.observeLastScan(),
        settings.settings,
        riskCounts,
    ) { threats, lastScan, prefs, (high, medium) ->
        HomeUiState(
            securityScore = scoreCalculator.calculate(
                SecurityScoreCalculator.Input(
                    activeThreats = threats.map { it.severity },
                    highRiskAppCount = high,
                    mediumRiskAppCount = medium,
                    lastScanAtMillis = lastScan?.finishedAt,
                    realtimeProtectionEnabled = prefs.realtimeProtectionEnabled,
                )
            ),
            activeThreats = threats.count { it.severity.ordinal >= Severity.MEDIUM.ordinal },
            highRiskApps = high,
            lastScanAt = lastScan?.finishedAt,
            onlineMode = prefs.onlineModeEnabled,
            signatureCount = 0,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _showScanOptions = MutableStateFlow(false)
    val showScanOptions: StateFlow<Boolean> = _showScanOptions.asStateFlow()

    init { refreshRiskCounts() }

    fun refreshRiskCounts() {
        viewModelScope.launch {
            runCatching {
                val whitelist = scanRepository.whitelistedPackages()
                val apps = inventory.riskInfo(whitelist, includeSystem = false)
                riskCounts.value = apps.count { it.riskLevel == RiskLevel.HIGH } to
                    apps.count { it.riskLevel == RiskLevel.MEDIUM }
            }
        }
    }

    fun openScanOptions() { _showScanOptions.value = true }
    fun dismissScanOptions() { _showScanOptions.value = false }

    fun startScan(type: ScanType, folderUri: Uri? = null) {
        _showScanOptions.value = false
        scanController.start(type, folderUri)
    }
}
