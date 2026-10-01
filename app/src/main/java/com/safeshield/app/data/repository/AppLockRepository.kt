package com.safeshield.app.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import com.safeshield.app.data.local.dao.LockedAppDao
import com.safeshield.app.data.local.entity.LockedAppEntity
import com.safeshield.app.data.prefs.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: LockedAppDao,
    private val settings: SettingsRepository,
) {
    /** Packages unlocked for this session, cleared when the screen turns off. */
    private val unlockedUntil = mutableMapOf<String, Long>()

    fun observeLockedApps(): Flow<List<LockedAppEntity>> = dao.observeAll()
    fun observeLockedPackages(): Flow<Set<String>> =
        dao.observeAll().map { list -> list.map { it.packageName }.toSet() }

    suspend fun lockedPackages(): Set<String> = dao.allPackages().toSet()

    suspend fun lock(packageName: String, label: String) =
        dao.add(LockedAppEntity(packageName, label))

    suspend fun unlock(packageName: String) = dao.remove(packageName)

    suspend fun setPin(pin: String) = settings.setPin(pin)
    suspend fun hasPin(): Boolean = settings.hasPin()
    suspend fun verifyPin(pin: String): Boolean = settings.verifyPin(pin)

    fun grantSession(packageName: String, durationMillis: Long = SESSION_MILLIS) {
        unlockedUntil[packageName] = System.currentTimeMillis() + durationMillis
    }

    fun isSessionValid(packageName: String): Boolean =
        (unlockedUntil[packageName] ?: 0L) > System.currentTimeMillis()

    fun clearSessions() = unlockedUntil.clear()

    // ---- Permission plumbing, explained to the user in the UI ----

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        return appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.contains("${context.packageName}/")
    }

    fun openUsageAccessSettings() = start(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    fun openAccessibilitySettings() = start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    fun openOverlaySettings() = start(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}"),
        )
    )

    /** Usage-stats fallback for devices where the user prefers not to enable accessibility. */
    fun foregroundPackageViaUsageStats(windowMillis: Long = 5_000L): String? {
        if (!hasUsageAccess()) return null
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - windowMillis, now)
        val event = android.app.usage.UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                last = event.packageName
            }
        }
        return last
    }

    suspend fun clearAll() {
        dao.clear()
        clearSessions()
    }

    private fun start(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    companion object {
        const val SESSION_MILLIS = 30_000L
    }
}
