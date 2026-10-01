package com.safeshield.app

import com.safeshield.app.domain.engine.SecurityScoreCalculator
import com.safeshield.app.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class SecurityScoreCalculatorTest {

    private val calculator = SecurityScoreCalculator()
    private val now = 1_750_000_000_000L

    @Test
    fun `clean freshly scanned device scores one hundred`() {
        val score = calculator.calculate(
            SecurityScoreCalculator.Input(lastScanAtMillis = now, nowMillis = now)
        )
        assertEquals(100, score)
    }

    @Test
    fun `critical threat drops the score sharply`() {
        val score = calculator.calculate(
            SecurityScoreCalculator.Input(
                activeThreats = listOf(Severity.CRITICAL),
                lastScanAtMillis = now,
                nowMillis = now,
            )
        )
        assertEquals(60, score)
    }

    @Test
    fun `never scanned costs twenty points`() {
        val score = calculator.calculate(
            SecurityScoreCalculator.Input(lastScanAtMillis = null, nowMillis = now)
        )
        assertEquals(80, score)
    }

    @Test
    fun `stale scan degrades the score`() {
        val old = now - TimeUnit.DAYS.toMillis(45)
        val score = calculator.calculate(
            SecurityScoreCalculator.Input(lastScanAtMillis = old, nowMillis = now)
        )
        assertEquals(80, score)
    }

    @Test
    fun `score is clamped to zero`() {
        val score = calculator.calculate(
            SecurityScoreCalculator.Input(
                activeThreats = List(10) { Severity.CRITICAL },
                highRiskAppCount = 20,
                realtimeProtectionEnabled = false,
                lastScanAtMillis = null,
                nowMillis = now,
            )
        )
        assertEquals(0, score)
    }

    @Test
    fun `disabling realtime protection costs points`() {
        val withRt = calculator.calculate(
            SecurityScoreCalculator.Input(lastScanAtMillis = now, nowMillis = now)
        )
        val withoutRt = calculator.calculate(
            SecurityScoreCalculator.Input(
                lastScanAtMillis = now,
                realtimeProtectionEnabled = false,
                nowMillis = now,
            )
        )
        assertTrue(withoutRt < withRt)
    }
}
