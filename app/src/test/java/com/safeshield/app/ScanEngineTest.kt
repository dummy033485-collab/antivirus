package com.safeshield.app

import android.content.Context
import com.safeshield.app.core.Hashing
import com.safeshield.app.data.local.entity.MalwareSignatureEntity
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.prefs.UserSettings
import com.safeshield.app.data.repository.AppInventoryRepository
import com.safeshield.app.data.repository.ScanRepository
import com.safeshield.app.data.repository.SignatureRepository
import com.safeshield.app.data.repository.VirusTotalRepository
import com.safeshield.app.domain.engine.HeuristicAnalyzer
import com.safeshield.app.domain.engine.ScanEngine
import com.safeshield.app.domain.model.ScanTarget
import com.safeshield.app.domain.model.Severity
import com.safeshield.app.domain.model.ThreatSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ScanEngineTest {

    private val context: Context = mockk(relaxed = true)
    private val inventory: AppInventoryRepository = mockk(relaxed = true)
    private val signatures: SignatureRepository = mockk(relaxed = true)
    private val virusTotal: VirusTotalRepository = mockk(relaxed = true)
    private val scanRepository: ScanRepository = mockk(relaxed = true)
    private val settings: SettingsRepository = mockk(relaxed = true)
    private val heuristics = HeuristicAnalyzer()

    private lateinit var engine: ScanEngine
    private lateinit var apk: File

    @Before
    fun setUp() {
        apk = File.createTempFile("sample", ".apk").apply { writeText("malicious-payload") }
        coEvery { settings.current() } returns UserSettings(onlineModeEnabled = false)
        coEvery { scanRepository.whitelistedPackages() } returns emptySet()
        coEvery { signatures.lookup(any()) } returns null
        every { inventory.packageInfoOf(any()) } returns null
        every { virusTotal.isConfigured } returns false

        engine = ScanEngine(
            context = context,
            inventory = inventory,
            signatures = signatures,
            heuristics = heuristics,
            virusTotal = virusTotal,
            scanRepository = scanRepository,
            settings = settings,
            ioDispatcher = StandardTestDispatcher(),
        )
    }

    private fun target() = ScanTarget(
        id = apk.absolutePath,
        label = apk.name,
        apkPath = apk.absolutePath,
        isInstalledApp = false,
        sizeBytes = apk.length(),
    )

    @Test
    fun `clean file produces no finding`() = runTest {
        assertNull(engine.inspect(target(), onlineEnabled = false))
    }

    @Test
    fun `known hash is detected offline`() = runTest {
        val hash = Hashing.sha256(apk)!!
        coEvery { signatures.lookup(hash) } returns MalwareSignatureEntity(
            sha256 = hash,
            malwareName = "Android.Trojan.Test",
            severity = Severity.CRITICAL,
        )

        val finding = engine.inspect(target(), onlineEnabled = false)

        assertNotNull(finding)
        assertEquals(Severity.CRITICAL, finding!!.severity)
        assertEquals(ThreatSource.SIGNATURE, finding.source)
        assertEquals("Android.Trojan.Test", finding.malwareName)
        assertEquals(hash, finding.sha256)
    }

    @Test
    fun `signature detection wins over online lookup`() = runTest {
        val hash = Hashing.sha256(apk)!!
        coEvery { signatures.lookup(hash) } returns MalwareSignatureEntity(
            sha256 = hash, malwareName = "Local.Hit", severity = Severity.HIGH,
        )
        coEvery { virusTotal.lookup(any()) } returns VirusTotalRepository.Verdict(50, 0, "vt")

        val finding = engine.inspect(target(), onlineEnabled = true)

        assertEquals(ThreatSource.SIGNATURE, finding?.source)
    }

    @Test
    fun `virustotal verdict is used when enabled and hash unknown locally`() = runTest {
        coEvery { virusTotal.lookup(any()) } returns
            VirusTotalRepository.Verdict(malicious = 12, suspicious = 1, label = "trojan.banker")

        val finding = engine.inspect(target(), onlineEnabled = true)

        assertNotNull(finding)
        assertEquals(ThreatSource.VIRUSTOTAL, finding!!.source)
        assertEquals(Severity.CRITICAL, finding.severity)
        assertTrue(finding.reasons.first().contains("12"))
    }

    @Test
    fun `online lookup is skipped when the user has not opted in`() = runTest {
        coEvery { virusTotal.lookup(any()) } returns
            VirusTotalRepository.Verdict(malicious = 40, suspicious = 0, label = "x")

        assertNull(engine.inspect(target(), onlineEnabled = false))
    }

    @Test
    fun `unreadable file does not crash the scan`() = runTest {
        val missing = ScanTarget("/nope/x.apk", "x.apk", "/nope/x.apk", false, 0)
        assertNull(engine.inspect(missing, onlineEnabled = false))
    }
}
