package com.safeshield.app.domain.model

/** What the user asked us to scan. */
enum class ScanType { QUICK, FULL, CUSTOM, REALTIME }

/** Ordered from harmless to worst – `ordinal` is used for sorting and scoring. */
enum class Severity { CLEAN, LOW, MEDIUM, HIGH, CRITICAL }

enum class RiskLevel { LOW, MEDIUM, HIGH }

enum class ThreatSource { SIGNATURE, HEURISTIC, VIRUSTOTAL }

enum class ScanState { IDLE, RUNNING, PAUSED, COMPLETED, CANCELLED, FAILED }

/** One scannable unit: an installed package or a standalone APK file. */
data class ScanTarget(
    val id: String,              // packageName or file path
    val label: String,           // user visible name
    val apkPath: String?,        // null when unreadable
    val isInstalledApp: Boolean,
    val sizeBytes: Long = 0L,
)

data class ThreatFinding(
    val targetId: String,
    val label: String,
    val packageName: String?,
    val filePath: String?,
    val sha256: String?,
    val severity: Severity,
    val source: ThreatSource,
    val malwareName: String?,
    /** Plain-language reasons shown to non-technical users. */
    val reasons: List<String>,
)

data class ScanProgress(
    val state: ScanState = ScanState.IDLE,
    val scanType: ScanType = ScanType.QUICK,
    val total: Int = 0,
    val scanned: Int = 0,
    val threats: Int = 0,
    val currentItem: String = "",
    val startedAt: Long = 0L,
    val elapsedMillis: Long = 0L,
    val etaMillis: Long = 0L,
    val error: String? = null,
) {
    val percent: Int
        get() = if (total <= 0) 0 else ((scanned.toFloat() / total) * 100f).toInt().coerceIn(0, 100)

    val isActive: Boolean get() = state == ScanState.RUNNING || state == ScanState.PAUSED
}

data class ScanSummary(
    val scanId: Long,
    val scanType: ScanType,
    val itemsScanned: Int,
    val threatsFound: Int,
    val durationMillis: Long,
    val finishedAt: Long,
    val findings: List<ThreatFinding>,
)

data class AppRiskInfo(
    val packageName: String,
    val label: String,
    val versionName: String,
    val installerPackage: String?,
    val isSystemApp: Boolean,
    val installedAt: Long,
    val riskScore: Int,              // 0..100
    val riskLevel: RiskLevel,
    /** Resource ids of plain-language explanations, resolved in the UI layer. */
    val reasonResIds: List<Int>,
    val dangerousPermissions: List<String>,
    val hasLauncherIcon: Boolean,
    val isDeviceAdmin: Boolean,
    val isAccessibilityService: Boolean,
    val isWhitelisted: Boolean = false,
)

data class WifiSecurityInfo(
    val connected: Boolean,
    val ssid: String?,
    val encryption: String,          // OPEN / WEP / WPA / WPA2 / WPA3 / UNKNOWN
    val rating: Int,                 // 0..100
    val level: RiskLevel,
)

data class JunkItem(
    val packageName: String?,
    val label: String,
    val path: String?,
    val sizeBytes: Long,
    val deletableByApp: Boolean,
)

data class LinkVerdict(
    val url: String,
    val safe: Boolean,
    val threatTypes: List<String>,
    val checkedAt: Long,
)
