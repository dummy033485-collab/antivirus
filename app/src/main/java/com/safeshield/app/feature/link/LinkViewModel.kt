package com.safeshield.app.feature.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.repository.LinkCheckRepository
import com.safeshield.app.domain.model.LinkVerdict
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LinkUiResult {
    data class Safe(val verdict: LinkVerdict) : LinkUiResult
    data class Unsafe(val verdict: LinkVerdict) : LinkUiResult
    data object Invalid : LinkUiResult
    data object NeedsOnline : LinkUiResult
    data object NotConfigured : LinkUiResult
    data class Error(val message: String) : LinkUiResult
}

@HiltViewModel
class LinkViewModel @Inject constructor(
    private val repository: LinkCheckRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _result = MutableStateFlow<LinkUiResult?>(null)
    val result: StateFlow<LinkUiResult?> = _result.asStateFlow()

    fun onUrlChange(value: String) {
        _url.value = value
        _result.value = null
    }

    fun check() {
        val target = _url.value
        viewModelScope.launch {
            _loading.value = true
            _result.value = when {
                !settings.current().onlineModeEnabled -> LinkUiResult.NeedsOnline
                else -> when (val outcome = repository.check(target)) {
                    is LinkCheckRepository.Outcome.Checked ->
                        if (outcome.verdict.safe) LinkUiResult.Safe(outcome.verdict)
                        else LinkUiResult.Unsafe(outcome.verdict)
                    LinkCheckRepository.Outcome.InvalidUrl -> LinkUiResult.Invalid
                    LinkCheckRepository.Outcome.NotConfigured -> LinkUiResult.NotConfigured
                    is LinkCheckRepository.Outcome.Failed -> LinkUiResult.Error(outcome.message)
                }
            }
            _loading.value = false
        }
    }
}
