package com.safeshield.app.feature.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.data.repository.WifiSecurityRepository
import com.safeshield.app.domain.model.WifiSecurityInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WifiViewModel @Inject constructor(
    private val repository: WifiSecurityRepository,
) : ViewModel() {

    private val _info = MutableStateFlow<WifiSecurityInfo?>(null)
    val info: StateFlow<WifiSecurityInfo?> = _info.asStateFlow()

    private val _needsLocation = MutableStateFlow(false)
    val needsLocation: StateFlow<Boolean> = _needsLocation.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _needsLocation.value = !repository.hasLocationPermission()
            _info.value = runCatching { repository.currentNetwork() }.getOrNull()
        }
    }
}
