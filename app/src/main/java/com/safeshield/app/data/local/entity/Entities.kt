package com.safeshield.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.safeshield.app.domain.model.ScanType
import com.safeshield.app.domain.model.Severity
import com.safeshield.app.domain.model.ThreatSource

@Entity(tableName = "malware_signatures", indices = [Index(value = ["sha256"], unique = true)])
data class MalwareSignatureEntity(
    @PrimaryKey val sha256: String,
    val malwareName: String,
    val severity: Severity,
    val platform: String = "android",
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "scan_records")
data class ScanRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanType: ScanType,
    val startedAt: Long,
    val finishedAt: Long,
    val itemsScanned: Int,
    val threatsFound: Int,
    val durationMillis: Long,
    val onlineModeUsed: Boolean,
)

@Entity(
    tableName = "threat_records",
    foreignKeys = [
        ForeignKey(
            entity = ScanRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("scanId"), Index("packageName")],
)
data class ThreatRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanId: Long,
    val label: String,
    val packageName: String?,
    val filePath: String?,
    val sha256: String?,
    val severity: Severity,
    val source: ThreatSource,
    val malwareName: String?,
    val reasons: List<String>,
    val resolved: Boolean = false,
    val detectedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "whitelist")
data class WhitelistEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "locked_apps")
data class LockedAppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val lockedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "link_checks")
data class LinkCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val safe: Boolean,
    val threatTypes: List<String>,
    val checkedAt: Long = System.currentTimeMillis(),
)
