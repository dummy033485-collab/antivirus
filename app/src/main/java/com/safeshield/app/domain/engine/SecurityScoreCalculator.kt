package com.safeshield.app.domain.engine

import com.safeshield.app.domain.model.Severity
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Produces the 0..100 number behind the big ring on the Home screen. */
@Singleton
class SecurityScoreCalculator @Inject constructor() {

    data class Input(
        val activeThreats: List<Severity> = emptyList(),
        val highRiskAppCount: Int = 0,
        val mediumRiskAppCount: Int = 0,
        val lastScanAtMillis: Long? = null,
        val realtimeProtectionEnabled: Boolean = true,
        val nowMillis: Long = System.currentTimeMillis(),
    )

    fun calculate(input: Input): Int {
        var score = 100

        input.activeThreats.forEach {
            score -= when (it) {
                Severity.CRITICAL -> 40
                Severity.HIGH -> 25
                Severity.MEDIUM -> 12
                Severity.LOW -> 5
                Severity.CLEAN -> 0
            }
        }

        score -= (input.highRiskAppCount * 6).coerceAtMost(24)
        score -= (input.mediumRiskAppCount * 2).coerceAtMost(10)

        if (!input.realtimeProtectionEnabled) score -= 10

        val last = input.lastScanAtMillis
        score -= when {
            last == null -> 20
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(input.nowMillis - last)
                when {
                    days <= 1 -> 0
                    days <= 7 -> 5
                    days <= 30 -> 12
                    else -> 20
                }
            }
        }

        return score.coerceIn(0, 100)
    }
}
