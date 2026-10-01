package com.safeshield.app

import com.safeshield.app.domain.engine.HeuristicAnalyzer
import com.safeshield.app.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HeuristicAnalyzerTest {

    private lateinit var analyzer: HeuristicAnalyzer

    @Before fun setUp() { analyzer = HeuristicAnalyzer() }

    private fun candidate(
        permissions: Set<String> = emptySet(),
        unknownSource: Boolean = false,
        launcher: Boolean = true,
        admin: Boolean = false,
        accessibility: Boolean = false,
        system: Boolean = false,
    ) = HeuristicAnalyzer.Candidate(
        permissions = permissions,
        installedFromUnknownSource = unknownSource,
        hasLauncherIcon = launcher,
        isDeviceAdmin = admin,
        isAccessibilityService = accessibility,
        isSystemApp = system,
    )

    @Test
    fun `plain app is clean`() {
        val verdict = analyzer.analyze(candidate(setOf("android.permission.INTERNET")))
        assertEquals(Severity.CLEAN, verdict.severity)
        assertTrue(verdict.reasons.isEmpty())
    }

    @Test
    fun `banking trojan pattern is flagged high or worse`() {
        val verdict = analyzer.analyze(
            candidate(
                permissions = setOf(
                    "android.permission.READ_SMS",
                    "android.permission.SYSTEM_ALERT_WINDOW",
                )
            )
        )
        assertTrue(verdict.severity.ordinal >= Severity.HIGH.ordinal)
        assertTrue(verdict.reasons.any { it.contains("banking-trojan") })
    }

    @Test
    fun `sideloaded dropper with hidden icon is critical`() {
        val verdict = analyzer.analyze(
            candidate(
                permissions = setOf("android.permission.REQUEST_INSTALL_PACKAGES"),
                unknownSource = true,
                launcher = false,
            )
        )
        assertEquals(Severity.CRITICAL, verdict.severity)
    }

    @Test
    fun `system apps are never flagged by heuristics`() {
        val verdict = analyzer.analyze(
            candidate(
                permissions = setOf(
                    "android.permission.READ_SMS",
                    "android.permission.SYSTEM_ALERT_WINDOW",
                ),
                system = true,
            )
        )
        assertEquals(Severity.CLEAN, verdict.severity)
    }

    @Test
    fun `accessibility abuse on a sideloaded app is reported`() {
        val verdict = analyzer.analyze(candidate(unknownSource = true, accessibility = true))
        assertTrue(verdict.severity.ordinal >= Severity.MEDIUM.ordinal)
        assertTrue(verdict.reasons.any { it.contains("Accessibility") })
    }
}
