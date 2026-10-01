package com.safeshield.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.safeshield.app.core.Hashing
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage(val tag: String) { ENGLISH("en"), URDU("ur") }
enum class ScanSchedule { OFF, DAILY, WEEKLY }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val onlineModeEnabled: Boolean = false,
    val realtimeProtectionEnabled: Boolean = true,
    val schedule: ScanSchedule = ScanSchedule.WEEKLY,
    val adsRemoved: Boolean = false,
    val appLockEnabled: Boolean = false,
    val biometricUnlockEnabled: Boolean = true,
    val onboardingDone: Boolean = false,
    val signatureVersion: Int = 0,
    val signatureUpdatedAt: Long = 0L,
)

private val Context.dataStore by preferencesDataStore(name = "safeshield_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val ONLINE = booleanPreferencesKey("online_mode")
        val REALTIME = booleanPreferencesKey("realtime")
        val SCHEDULE = stringPreferencesKey("schedule")
        val ADS_REMOVED = booleanPreferencesKey("ads_removed")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val SIG_VERSION = intPreferencesKey("sig_version")
        val SIG_UPDATED = longPreferencesKey("sig_updated")
    }

    val settings: Flow<UserSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            UserSettings(
                themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
                language = p[Keys.LANGUAGE]?.let { tag -> AppLanguage.entries.firstOrNull { it.tag == tag } } ?: AppLanguage.ENGLISH,
                onlineModeEnabled = p[Keys.ONLINE] ?: false,
                realtimeProtectionEnabled = p[Keys.REALTIME] ?: true,
                schedule = p[Keys.SCHEDULE]?.let { runCatching { ScanSchedule.valueOf(it) }.getOrNull() } ?: ScanSchedule.WEEKLY,
                adsRemoved = p[Keys.ADS_REMOVED] ?: false,
                appLockEnabled = p[Keys.APP_LOCK] ?: false,
                biometricUnlockEnabled = p[Keys.BIOMETRIC] ?: true,
                onboardingDone = p[Keys.ONBOARDING] ?: false,
                signatureVersion = p[Keys.SIG_VERSION] ?: 0,
                signatureUpdatedAt = p[Keys.SIG_UPDATED] ?: 0L,
            )
        }

    suspend fun current(): UserSettings = settings.first()

    suspend fun setTheme(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }
    suspend fun setLanguage(language: AppLanguage) = edit { it[Keys.LANGUAGE] = language.tag }
    suspend fun setOnlineMode(enabled: Boolean) = edit { it[Keys.ONLINE] = enabled }
    suspend fun setRealtime(enabled: Boolean) = edit { it[Keys.REALTIME] = enabled }
    suspend fun setSchedule(schedule: ScanSchedule) = edit { it[Keys.SCHEDULE] = schedule.name }
    suspend fun setAdsRemoved(removed: Boolean) = edit { it[Keys.ADS_REMOVED] = removed }
    suspend fun setAppLockEnabled(enabled: Boolean) = edit { it[Keys.APP_LOCK] = enabled }
    suspend fun setBiometricUnlock(enabled: Boolean) = edit { it[Keys.BIOMETRIC] = enabled }
    suspend fun setOnboardingDone(done: Boolean) = edit { it[Keys.ONBOARDING] = done }

    suspend fun setSignatureMeta(version: Int, updatedAt: Long) = edit {
        it[Keys.SIG_VERSION] = version
        it[Keys.SIG_UPDATED] = updatedAt
    }

    // ----- App-lock PIN: stored only as a salted SHA-256 digest -----

    suspend fun setPin(pin: String) {
        val salt = Hashing.sha256(System.nanoTime().toString()).take(16)
        edit {
            it[Keys.PIN_SALT] = salt
            it[Keys.PIN_HASH] = Hashing.sha256(salt + pin)
        }
    }

    suspend fun hasPin(): Boolean = context.dataStore.data.first()[Keys.PIN_HASH] != null

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val salt = prefs[Keys.PIN_SALT] ?: return false
        val hash = prefs[Keys.PIN_HASH] ?: return false
        return Hashing.sha256(salt + pin) == hash
    }

    /** Wipes every preference, including the PIN. Used by "Delete all my data". */
    suspend fun clearAll() = context.dataStore.edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
