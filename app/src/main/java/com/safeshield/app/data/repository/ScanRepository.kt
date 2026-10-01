package com.safeshield.app.data.repository

import com.safeshield.app.data.local.dao.ScanHistoryDao
import com.safeshield.app.data.local.dao.WhitelistDao
import com.safeshield.app.data.local.entity.ScanRecordEntity
import com.safeshield.app.data.local.entity.ThreatRecordEntity
import com.safeshield.app.data.local.entity.WhitelistEntity
import com.safeshield.app.domain.model.ScanSummary
import com.safeshield.app.domain.model.ScanType
import com.safeshield.app.domain.model.ThreatFinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Persistence for scans, findings and the user's trust list. */
@Singleton
class ScanRepository @Inject constructor(
    private val historyDao: ScanHistoryDao,
    private val whitelistDao: WhitelistDao,
) {
    fun observeHistory(): Flow<List<ScanRecordEntity>> = historyDao.observeHistory()
    fun observeLastScan(): Flow<ScanRecordEntity?> = historyDao.observeLastScan()
    fun observeActiveThreats(): Flow<List<ThreatRecordEntity>> = historyDao.observeActiveThreats()
    fun observeWhitelist(): Flow<List<WhitelistEntity>> = whitelistDao.observeAll()
    fun observeWhitelistPackages(): Flow<Set<String>> =
        whitelistDao.observeAll().map { list -> list.map { it.packageName }.toSet() }

    suspend fun whitelistedPackages(): Set<String> = whitelistDao.allPackages().toSet()

    suspend fun saveScan(
        scanType: ScanType,
        startedAt: Long,
        finishedAt: Long,
        itemsScanned: Int,
        onlineUsed: Boolean,
        findings: List<ThreatFinding>,
    ): Long = historyDao.saveScan(
        ScanRecordEntity(
            scanType = scanType,
            startedAt = startedAt,
            finishedAt = finishedAt,
            itemsScanned = itemsScanned,
            threatsFound = findings.size,
            durationMillis = finishedAt - startedAt,
            onlineModeUsed = onlineUsed,
        )
    ) { scanId ->
        findings.map { f ->
            ThreatRecordEntity(
                scanId = scanId,
                label = f.label,
                packageName = f.packageName,
                filePath = f.filePath,
                sha256 = f.sha256,
                severity = f.severity,
                source = f.source,
                malwareName = f.malwareName,
                reasons = f.reasons,
            )
        }
    }

    suspend fun summary(scanId: Long): ScanSummary? {
        val record = historyDao.getScan(scanId) ?: return null
        val threats = historyDao.getThreatsForScan(scanId)
        return ScanSummary(
            scanId = record.id,
            scanType = record.scanType,
            itemsScanned = record.itemsScanned,
            threatsFound = record.threatsFound,
            durationMillis = record.durationMillis,
            finishedAt = record.finishedAt,
            findings = threats.map { it.toFinding() },
        )
    }

    fun observeThreats(scanId: Long): Flow<List<ThreatFinding>> =
        historyDao.observeThreatsForScan(scanId).map { list -> list.map { it.toFinding() } }

    suspend fun addToWhitelist(packageName: String, label: String) {
        whitelistDao.add(WhitelistEntity(packageName = packageName, label = label))
        historyDao.markResolvedByPackage(packageName)
    }

    suspend fun removeFromWhitelist(packageName: String) = whitelistDao.remove(packageName)
    suspend fun markResolved(threatId: Long) = historyDao.markResolved(threatId)
    suspend fun markResolvedByPackage(packageName: String) = historyDao.markResolvedByPackage(packageName)
    suspend fun clearHistory() = historyDao.clearHistory()

    suspend fun clearAll() {
        historyDao.clearHistory()
        whitelistDao.clear()
    }

    private fun ThreatRecordEntity.toFinding() = ThreatFinding(
        targetId = packageName ?: filePath.orEmpty(),
        label = label,
        packageName = packageName,
        filePath = filePath,
        sha256 = sha256,
        severity = severity,
        source = source,
        malwareName = malwareName,
        reasons = reasons,
    )
}
