package com.safeshield.app.feature.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.data.prefs.AppLanguage
import com.safeshield.app.data.prefs.ScanSchedule
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.prefs.ThemeMode
import com.safeshield.app.data.prefs.UserSettings
import com.safeshield.app.data.repository.AppLockRepository
import com.safeshield.app.data.repository.BillingRepository
import com.safeshield.app.data.repository.LinkCheckRepository
import com.safeshield.app.data.repository.ScanRepository
import com.safeshield.app.data.repository.SignatureRepository
import com.safeshield.app.worker.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val scanRepository: ScanRepository,
    private val signatures: SignatureRepository,
    private val appLock: AppLockRepository,
    private val linkChecks: LinkCheckRepository,
    private val scheduler: WorkScheduler,
    private val billing: BillingRepository,
) : ViewModel() {

    val state: StateFlow<UserSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    val signatureCount: StateFlow<Int> = signatures.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val price: StateFlow<String?> = billing.productPrice

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setTheme(mode) }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        settings.setLanguage(language)
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
            androidx.core.os.LocaleListCompat.forLanguageTags(language.tag)
        )
    }

    fun setOnlineMode(enabled: Boolean) = viewModelScope.launch { settings.setOnlineMode(enabled) }

    fun setRealtime(enabled: Boolean) = viewModelScope.launch {
        settings.setRealtime(enabled)
        if (enabled) scheduler.scheduleApkWatch()
    }

    fun setSchedule(schedule: ScanSchedule) = viewModelScope.launch {
        settings.setSchedule(schedule)
        scheduler.applySchedule(schedule)
    }

    fun updateSignatures() {
        scheduler.updateSignaturesNow()
        _message.value = "updating"
    }

    fun buyRemoveAds(activity: Activity) = billing.launchPurchase(activity)

    /** Irreversibly erases everything SafeShield stored on this device. */
    fun deleteAllData() {
        viewModelScope.launch {
            scanRepository.clearAll()
            appLock.clearAll()
            linkChecks.clear()
            signatures.clear()
            scheduler.cancelAll()
            settings.clearAll()
            signatures.seedIfEmpty()
            _message.value = "deleted"
        }
    }

    fun consumeMessage() { _message.value = null }
}
