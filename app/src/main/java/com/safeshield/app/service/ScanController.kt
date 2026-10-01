package com.safeshield.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.repository.ScanRepository
import com.safeshield.app.domain.engine.ScanEngine
import com.safeshield.app.domain.model.ScanProgress
import com.safeshield.app.domain.model.ScanState
import com.safeshield.app.domain.model.ScanType
import com.safeshield.app.domain.model.ThreatFinding
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide owner of the running scan. The UI observes [progress]; the
 * foreground service keeps the work alive when the user leaves the app.
 * Survives configuration changes and screen navigation.
 */
@Singleton
class ScanController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: ScanEngine,
    private val scanRepository: ScanRepository,
    private val settings: SettingsRepository,
    private val notifications: NotificationHelper,
) {
    private val scope = CoroutineScope(SupervisorJob())

    private val _progress = MutableStateFlow(ScanProgress())
    val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    private val _lastSummaryId = MutableStateFlow<Long?>(null)
    val lastSummaryId: StateFlow<Long?> = _lastSummaryId.asStateFlow()

    private val _findings = MutableStateFlow<List<ThreatFinding>>(emptyList())
    val findings: StateFlow<List<ThreatFinding>> = _findings.asStateFlow()

    private val paused = MutableStateFlow(false)
    private var job: Job? = null

    val isRunning: Boolean get() = job?.isActive == true

    fun start(type: ScanType, customFolderUri: Uri? = null) {
        if (isRunning) return
        _findings.value = emptyList()
        _lastSummaryId.value = null
        paused.value = false
        _progress.value = ScanProgress(
            state = ScanState.RUNNING,
            scanType = type,
            startedAt = System.currentTimeMillis(),
        )
        startService()

        job = scope.launch {
            try {
                val result = engine.scan(
                    request = ScanEngine.Request(type = type, customFolderUri = customFolderUri),
                    progress = { scanned, total, threats, item -> publish(scanned, total, threats, item) },
                    flowControl = { while (paused.value) delay(120) },
                )
                val scanId = scanRepository.saveScan(
                    scanType = type,
                    startedAt = result.startedAt,
                    finishedAt = result.finishedAt,
                    itemsScanned = result.itemsScanned,
                    onlineUsed = result.onlineUsed,
                    findings = result.findings,
                )
                _findings.value = result.findings
                _lastSummaryId.value = scanId
                _progress.update {
                    it.copy(
                        state = ScanState.COMPLETED,
                        scanned = result.itemsScanned,
                        total = result.itemsScanned,
                        threats = result.findings.size,
                        currentItem = "",
                        elapsedMillis = result.finishedAt - result.startedAt,
                        etaMillis = 0,
                    )
                }
                if (result.findings.isNotEmpty()) {
                    notifications.showThreatSummary(result.findings.size, result.findings.first().label)
                }
            } catch (c: CancellationException) {
                _progress.update { it.copy(state = ScanState.CANCELLED, currentItem = "") }
                throw c
            } catch (t: Throwable) {
                _progress.update {
                    it.copy(state = ScanState.FAILED, error = t.message ?: "Scan failed")
                }
            } finally {
                stopService()
            }
        }
    }

    /** Silent background scan (realtime / scheduled) — does not touch UI progress. */
    suspend fun scanSilently(type: ScanType, packageNames: List<String>? = null): List<ThreatFinding> {
        val result = engine.scan(ScanEngine.Request(type = type, packageNames = packageNames))
        if (result.itemsScanned > 0) {
            scanRepository.saveScan(
                scanType = type,
                startedAt = result.startedAt,
                finishedAt = result.finishedAt,
                itemsScanned = result.itemsScanned,
                onlineUsed = result.onlineUsed,
                findings = result.findings,
            )
        }
        return result.findings
    }

    fun pause() {
        if (!isRunning) return
        paused.value = true
        _progress.update { it.copy(state = ScanState.PAUSED) }
    }

    fun resume() {
        if (!isRunning) return
        paused.value = false
        _progress.update { it.copy(state = ScanState.RUNNING) }
    }

    fun cancel() {
        scope.launch { job?.cancelAndJoin() }
        paused.value = false
        _progress.update { it.copy(state = ScanState.CANCELLED, currentItem = "") }
        stopService()
    }

    fun acknowledgeResult() {
        _progress.value = ScanProgress()
    }

    private suspend fun publish(scanned: Int, total: Int, threats: Int, item: String) {
        val started = _progress.value.startedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        val elapsed = System.currentTimeMillis() - started
        val eta = if (scanned > 0 && total > scanned) {
            (elapsed.toDouble() / scanned * (total - scanned)).toLong()
        } else 0L

        _progress.update {
            it.copy(
                total = total,
                scanned = scanned,
                threats = threats,
                currentItem = item,
                elapsedMillis = elapsed,
                etaMillis = eta,
            )
        }
        notifications.updateScanProgress(scanned, total, item)
    }

    private fun startService() {
        val intent = Intent(context, ScanForegroundService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private fun stopService() {
        runCatching { context.stopService(Intent(context, ScanForegroundService::class.java)) }
    }

    private inline fun MutableStateFlow<ScanProgress>.update(block: (ScanProgress) -> ScanProgress) {
        value = block(value)
    }

    suspend fun onlineModeEnabled(): Boolean = settings.settings.first().onlineModeEnabled
}
