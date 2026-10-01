package com.safeshield.app.feature.result

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeshield.app.core.Formatters
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.repository.AppInventoryRepository
import com.safeshield.app.data.repository.ScanRepository
import com.safeshield.app.domain.model.ScanSummary
import com.safeshield.app.domain.model.ThreatFinding
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val scanRepository: ScanRepository,
    private val inventory: AppInventoryRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val scanId: Long = savedStateHandle.get<Long>("scanId") ?: 0L

    private val _summary = MutableStateFlow<ScanSummary?>(null)
    val summary: StateFlow<ScanSummary?> = _summary.asStateFlow()

    val adsRemoved: StateFlow<Boolean> = settings.settings
        .map { it.adsRemoved }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _exportedPath = MutableStateFlow<String?>(null)
    val exportedPath: StateFlow<String?> = _exportedPath.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch { _summary.value = scanRepository.summary(scanId) }
    }

    fun whitelist(finding: ThreatFinding) {
        val pkg = finding.packageName ?: return
        viewModelScope.launch {
            scanRepository.addToWhitelist(pkg, finding.label)
            load()
        }
    }

    fun uninstall(finding: ThreatFinding) {
        finding.packageName?.let(inventory::requestUninstall)
    }

    fun openAppInfo(finding: ThreatFinding) {
        finding.packageName?.let(inventory::openAppSettings)
    }

    fun deleteFile(finding: ThreatFinding) {
        val path = finding.filePath ?: return
        if (finding.packageName != null) return // installed apps go through uninstall
        viewModelScope.launch {
            runCatching { File(path).delete() }
            load()
        }
    }

    /** Writes a plain-text report into app-private storage, shareable via FileProvider. */
    fun exportReport() {
        val data = _summary.value ?: return
        viewModelScope.launch {
            runCatching {
                val dir = File(context.filesDir, "reports").apply { mkdirs() }
                val file = File(dir, "safeshield_report_${data.scanId}.txt")
                file.writeText(buildReport(data))
                _exportedPath.value = file.absolutePath
            }
        }
    }

    fun consumeExport() { _exportedPath.value = null }

    private fun buildReport(data: ScanSummary): String = buildString {
        appendLine("SafeShield scan report")
        appendLine("======================")
        appendLine("Scan type     : ${data.scanType}")
        appendLine("Finished      : ${Formatters.dateTime(data.finishedAt)}")
        appendLine("Items scanned : ${data.itemsScanned}")
        appendLine("Duration      : ${Formatters.duration(data.durationMillis)}")
        appendLine("Threats found : ${data.threatsFound}")
        appendLine()
        if (data.findings.isEmpty()) {
            appendLine("No threats were detected.")
        } else {
            data.findings.forEachIndexed { index, f ->
                appendLine("${index + 1}. ${f.label} [${f.severity}] (${f.source})")
                f.packageName?.let { appendLine("   package : $it") }
                f.filePath?.let { appendLine("   file    : $it") }
                f.sha256?.let { appendLine("   sha256  : $it") }
                f.malwareName?.let { appendLine("   name    : $it") }
                f.reasons.forEach { appendLine("   - $it") }
                appendLine()
            }
        }
        appendLine("Generated on-device. No data was uploaded.")
    }
}
