package com.safeshield.app.domain.engine

import com.safeshield.app.R
import com.safeshield.app.domain.model.RiskLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts an app's declared permissions and install traits into a 0..100 risk
 * score. Pure Kotlin on purpose: fully unit-testable with no Android framework.
 */
@Singleton
class RiskScorer @Inject constructor() {

    data class Input(
        val permissions: Set<String>,
        val installedFromUnknownSource: Boolean = false,
        val hasLauncherIcon: Boolean = true,
        val isDeviceAdmin: Boolean = false,
        val isAccessibilityService: Boolean = false,
        val isSystemApp: Boolean = false,
    )

    data class Output(
        val score: Int,
        val level: RiskLevel,
        val reasonResIds: List<Int>,
        val dangerousPermissions: List<String>,
    )

    /** Weight of each individually meaningful permission group. */
    private val weights: List<Rule> = listOf(
        Rule(setOf("android.permission.READ_SMS", "android.permission.RECEIVE_SMS", "android.permission.SEND_SMS"), 22, R.string.perm_sms),
        Rule(setOf("android.permission.READ_CALL_LOG", "android.permission.PROCESS_OUTGOING_CALLS"), 14, R.string.perm_call_log),
        Rule(setOf("android.permission.READ_CONTACTS"), 10, R.string.perm_contacts),
        Rule(setOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION"), 12, R.string.perm_location),
        Rule(setOf("android.permission.RECORD_AUDIO"), 14, R.string.perm_mic),
        Rule(setOf("android.permission.CAMERA"), 10, R.string.perm_camera),
        Rule(setOf("android.permission.SYSTEM_ALERT_WINDOW"), 16, R.string.perm_overlay),
        Rule(setOf("android.permission.REQUEST_INSTALL_PACKAGES"), 18, R.string.perm_install_packages),
    )

    fun score(input: Input): Output {
        var score = 0
        val reasons = mutableListOf<Int>()
        val dangerous = mutableListOf<String>()

        weights.forEach { rule ->
            val matched = rule.permissions.intersect(input.permissions)
            if (matched.isNotEmpty()) {
                score += rule.weight
                reasons += rule.reasonRes
                dangerous += matched
            }
        }

        if (input.isAccessibilityService) {
            score += 20; reasons += R.string.perm_accessibility
        }
        if (input.isDeviceAdmin) {
            score += 18; reasons += R.string.perm_device_admin
        }
        if (input.installedFromUnknownSource) {
            score += 15; reasons += R.string.perm_unknown_source
        }
        if (!input.hasLauncherIcon) {
            score += 20; reasons += R.string.perm_no_launcher
        }

        // System apps ship these permissions by design; damp their score so the
        // list is not drowned in false positives.
        if (input.isSystemApp) score = (score * 0.4f).toInt()

        val clamped = score.coerceIn(0, 100)
        return Output(
            score = clamped,
            level = levelFor(clamped),
            reasonResIds = reasons.distinct(),
            dangerousPermissions = dangerous.distinct().sorted(),
        )
    }

    fun levelFor(score: Int): RiskLevel = when {
        score >= 60 -> RiskLevel.HIGH
        score >= 30 -> RiskLevel.MEDIUM
        else -> RiskLevel.LOW
    }

    private data class Rule(val permissions: Set<String>, val weight: Int, val reasonRes: Int)
}
