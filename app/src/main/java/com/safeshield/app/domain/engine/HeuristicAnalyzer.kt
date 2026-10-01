package com.safeshield.app.domain.engine

import com.safeshield.app.domain.model.Severity
import com.safeshield.app.domain.model.ThreatSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline behavioural detection. Catches repackaged malware whose hash is not in
 * the signature DB yet, which is exactly where most free scanners give up when
 * they have no network.
 */
@Singleton
class HeuristicAnalyzer @Inject constructor() {

    data class Candidate(
        val permissions: Set<String>,
        val installedFromUnknownSource: Boolean,
        val hasLauncherIcon: Boolean,
        val isDeviceAdmin: Boolean,
        val isAccessibilityService: Boolean,
        val isSystemApp: Boolean,
        val isDebuggable: Boolean = false,
        val targetSdk: Int = 35,
    )

    data class Verdict(
        val severity: Severity,
        val reasons: List<String>,
        val source: ThreatSource = ThreatSource.HEURISTIC,
    )

    private companion object {
        const val SMS_READ = "android.permission.READ_SMS"
        const val SMS_RECEIVE = "android.permission.RECEIVE_SMS"
        const val SMS_SEND = "android.permission.SEND_SMS"
        const val OVERLAY = "android.permission.SYSTEM_ALERT_WINDOW"
        const val INSTALL = "android.permission.REQUEST_INSTALL_PACKAGES"
        const val ACCESSIBILITY_BIND = "android.permission.BIND_ACCESSIBILITY_SERVICE"
        const val CONTACTS = "android.permission.READ_CONTACTS"
        const val RECORD = "android.permission.RECORD_AUDIO"
        const val BOOT = "android.permission.RECEIVE_BOOT_COMPLETED"
    }

    fun analyze(c: Candidate): Verdict {
        if (c.isSystemApp) return Verdict(Severity.CLEAN, emptyList())

        val reasons = mutableListOf<String>()
        var points = 0

        val hasSms = c.permissions.any { it in setOf(SMS_READ, SMS_RECEIVE, SMS_SEND) }

        // Classic banking-trojan combo: read one-time codes + draw a fake login.
        if (hasSms && c.permissions.contains(OVERLAY)) {
            points += 45
            reasons += "Can read your SMS codes and draw a fake screen over other apps — the standard banking-trojan pattern."
        }
        // Dropper pattern.
        if (c.permissions.contains(INSTALL) && c.installedFromUnknownSource) {
            points += 35
            reasons += "Came from outside the Play Store and can install more apps by itself."
        }
        // Hidden app.
        if (!c.hasLauncherIcon) {
            points += 30
            reasons += "Has no icon in your app list, so it runs without you seeing it."
        }
        // Full dropper profile: invisible, sideloaded, and able to install more apps.
        // Each trait alone is suspicious; together they are near-certain malware.
        if (!c.hasLauncherIcon && c.installedFromUnknownSource &&
            c.permissions.contains(INSTALL)
        ) {
            points += 15
            reasons += "Hides itself, came from outside the Play Store, and can install more apps — a dropper."
        }
        // Hard-to-remove.
        if (c.isDeviceAdmin && c.installedFromUnknownSource) {
            points += 30
            reasons += "Took device-administrator rights, which makes it hard to uninstall."
        }
        // Accessibility abuse.
        if (c.isAccessibilityService && c.installedFromUnknownSource) {
            points += 35
            reasons += "Uses the Accessibility service to read your screen and tap for you."
        }
        // Spyware-ish bundle.
        if (c.permissions.contains(RECORD) && c.permissions.contains(CONTACTS) && !c.hasLauncherIcon) {
            points += 25
            reasons += "Can record audio and read contacts while staying hidden."
        }
        if (c.permissions.contains(BOOT) && !c.hasLauncherIcon) {
            points += 10
            reasons += "Restarts itself every time your phone boots."
        }
        if (c.isDebuggable) {
            points += 10
            reasons += "Is a debuggable build, which is unusual for a real release."
        }
        // Very old targetSdk sidesteps modern runtime restrictions.
        if (c.targetSdk in 1..22) {
            points += 15
            reasons += "Targets a very old Android version to avoid today's security rules."
        }

        val severity = when {
            points >= 70 -> Severity.CRITICAL
            points >= 45 -> Severity.HIGH
            points >= 25 -> Severity.MEDIUM
            points >= 10 -> Severity.LOW
            else -> Severity.CLEAN
        }
        return Verdict(severity, if (severity == Severity.CLEAN) emptyList() else reasons)
    }
}
