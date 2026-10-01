package com.safeshield.app.feature.applock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.repository.AppInventoryRepository
import com.safeshield.app.data.repository.AppLockRepository
import com.safeshield.app.domain.model.AppRiskInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppLockUiState(
    val enabled: Boolean = false,
    val hasPin: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val hasAccessibility: Boolean = false,
    val apps: List<AppRiskInfo> = emptyList(),
    val lockedPackages: Set<String> = emptySet(),
    val loading: Boolean = true,
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLock: AppLockRepository,
    private val inventory: AppInventoryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AppLockUiState())
    val state: StateFlow<AppLockUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val prefs = settings.current()
            val apps = runCatching { inventory.riskInfo(includeSystem = false) }.getOrDefault(emptyList())
            _state.value = AppLockUiState(
                enabled = prefs.appLockEnabled,
                hasPin = appLock.hasPin(),
                hasUsageAccess = appLock.hasUsageAccess(),
                hasAccessibility = appLock.isAccessibilityEnabled(),
                apps = apps.sortedBy { it.label.lowercase() },
                lockedPackages = appLock.lockedPackages(),
                loading = false,
            )
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setAppLockEnabled(enabled)
            refresh()
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            appLock.setPin(pin)
            refresh()
        }
    }

    fun toggleLock(app: AppRiskInfo) {
        viewModelScope.launch {
            if (app.packageName in _state.value.lockedPackages) appLock.unlock(app.packageName)
            else appLock.lock(app.packageName, app.label)
            refresh()
        }
    }

    fun openUsageAccess() = appLock.openUsageAccessSettings()
    fun openAccessibility() = appLock.openAccessibilitySettings()
}
