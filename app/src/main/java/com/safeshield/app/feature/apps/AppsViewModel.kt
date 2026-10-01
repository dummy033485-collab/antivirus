package com.safeshield.app.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.data.repository.AppInventoryRepository
import com.safeshield.app.data.repository.ScanRepository
import com.safeshield.app.domain.model.AppRiskInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppsUiState(
    val apps: List<AppRiskInfo> = emptyList(),
    val query: String = "",
    val showSystem: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
) {
    val visible: List<AppRiskInfo>
        get() = apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
}

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val inventory: AppInventoryRepository,
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AppsUiState())
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val whitelist = scanRepository.whitelistedPackages()
                inventory.riskInfo(whitelist, includeSystem = _state.value.showSystem)
            }.onSuccess { apps ->
                _state.value = _state.value.copy(apps = apps, loading = false)
            }.onFailure { t ->
                _state.value = _state.value.copy(loading = false, error = t.message)
            }
        }
    }

    fun onQueryChange(query: String) { _state.value = _state.value.copy(query = query) }

    fun toggleSystemApps() {
        _state.value = _state.value.copy(showSystem = !_state.value.showSystem)
        refresh()
    }

    fun openAppInfo(app: AppRiskInfo) = inventory.openAppSettings(app.packageName)
    fun uninstall(app: AppRiskInfo) = inventory.requestUninstall(app.packageName)

    fun toggleWhitelist(app: AppRiskInfo) {
        viewModelScope.launch {
            if (app.isWhitelisted) scanRepository.removeFromWhitelist(app.packageName)
            else scanRepository.addToWhitelist(app.packageName, app.label)
            refresh()
        }
    }
}
