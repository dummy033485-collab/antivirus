package com.safeshield.app

import com.safeshield.app.domain.engine.RiskScorer
import com.safeshield.app.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RiskScorerTest {

    private lateinit var scorer: RiskScorer

    @Before fun setUp() { scorer = RiskScorer() }

    @Test
    fun `app with no permissions is low risk`() {
        val result = scorer.score(RiskScorer.Input(permissions = emptySet()))
        assertEquals(0, result.score)
        assertEquals(RiskLevel.LOW, result.level)
        assertTrue(result.reasonResIds.isEmpty())
    }

    @Test
    fun `sms plus overlay plus sideload is high risk`() {
        val result = scorer.score(
            RiskScorer.Input(
                permissions = setOf(
                    "android.permission.READ_SMS",
                    "android.permission.SYSTEM_ALERT_WINDOW",
                ),
                installedFromUnknownSource = true,
            )
        )
        assertEquals(RiskLevel.HIGH, result.level)
        assertTrue(result.score >= 60)
    }

    @Test
    fun `hidden launcher icon adds a reason`() {
        val result = scorer.score(RiskScorer.Input(permissions = emptySet(), hasLauncherIcon = false))
        assertTrue(result.reasonResIds.contains(R.string.perm_no_launcher))
    }

    @Test
    fun `system apps are damped so they do not flood the list`() {
        val permissions = setOf(
            "android.permission.READ_SMS",
            "android.permission.CAMERA",
            "android.permission.ACCESS_FINE_LOCATION",
        )
        val user = scorer.score(RiskScorer.Input(permissions = permissions, isSystemApp = false))
        val system = scorer.score(RiskScorer.Input(permissions = permissions, isSystemApp = true))
        assertTrue(system.score < user.score)
    }

    @Test
    fun `score never exceeds one hundred`() {
        val result = scorer.score(
            RiskScorer.Input(
                permissions = setOf(
                    "android.permission.READ_SMS",
                    "android.permission.SEND_SMS",
                    "android.permission.READ_CALL_LOG",
                    "android.permission.READ_CONTACTS",
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.RECORD_AUDIO",
                    "android.permission.CAMERA",
                    "android.permission.SYSTEM_ALERT_WINDOW",
                    "android.permission.REQUEST_INSTALL_PACKAGES",
                ),
                installedFromUnknownSource = true,
                hasLauncherIcon = false,
                isDeviceAdmin = true,
                isAccessibilityService = true,
            )
        )
        assertEquals(100, result.score)
    }

    @Test
    fun `level boundaries are stable`() {
        assertEquals(RiskLevel.LOW, scorer.levelFor(29))
        assertEquals(RiskLevel.MEDIUM, scorer.levelFor(30))
        assertEquals(RiskLevel.MEDIUM, scorer.levelFor(59))
        assertEquals(RiskLevel.HIGH, scorer.levelFor(60))
    }
}
