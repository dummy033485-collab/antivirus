package com.safeshield.app.feature.applock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.repository.AppLockRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Only reads WHICH package came to the foreground – never window content.
 * This is disclosed verbatim to the user in accessibility_service_config.xml
 * and on the App Lock screen, as Google Play's Accessibility policy requires.
 */
@AndroidEntryPoint
class AppLockAccessibilityService : AccessibilityService() {

    @Inject lateinit var appLock: AppLockRepository
    @Inject lateinit var settings: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastHandled: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == lastHandled) return

        scope.launch {
            if (!settings.current().appLockEnabled) return@launch
            if (pkg !in appLock.lockedPackages()) {
                lastHandled = null
                return@launch
            }
            if (appLock.isSessionValid(pkg)) return@launch
            lastHandled = pkg
            launchLockScreen(pkg)
        }
    }

    private fun launchLockScreen(packageName: String) {
        val intent = Intent(this, LockScreenActivity::class.java)
            .putExtra(LockScreenActivity.EXTRA_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() = Unit
}
