package com.safeshield.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.prefs.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _sharedLink = MutableStateFlow<String?>(null)
    val sharedLink: StateFlow<String?> = _sharedLink.asStateFlow()

    fun onSharedLink(text: String) {
        val candidate = text.trim().split(Regex("\\s+")).firstOrNull { it.contains('.') }
        _sharedLink.value = candidate
    }

    fun consumeSharedLink() { _sharedLink.value = null }
}
