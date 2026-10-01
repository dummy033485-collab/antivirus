package com.safeshield.app.feature.junk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.data.repository.JunkRepository
import com.safeshield.app.domain.model.JunkItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JunkUiState(
    val items: List<JunkItem> = emptyList(),
    val loading: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val freedBytes: Long? = null,
) {
    val totalBytes: Long get() = items.sumOf { it.sizeBytes }
    val ownCleanableBytes: Long get() = items.filter { it.deletableByApp }.sumOf { it.sizeBytes }
}

@HiltViewModel
class JunkViewModel @Inject constructor(
    private val repository: JunkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(JunkUiState())
    val state: StateFlow<JunkUiState> = _state.asStateFlow()

    init { scan() }

    fun scan() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                hasUsageAccess = repository.hasUsageStatsPermission(),
            )
            val items = runCatching { repository.findJunk() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(items = items, loading = false)
        }
    }

    fun cleanOwnJunk() {
        viewModelScope.launch {
            val freed = repository.cleanOwnJunk()
            _state.value = _state.value.copy(freedBytes = freed)
            scan()
        }
    }

    fun openStorageSettings(packageName: String?) = repository.openStorageSettings(packageName)
    fun openUsageAccess() = repository.openUsageAccessSettings()
    fun consumeFreed() { _state.value = _state.value.copy(freedBytes = null) }
}
