package com.safeshield.app.domain.engine

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.safeshield.app.core.Hashing
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.repository.AppInventoryRepository
import com.safeshield.app.data.repository.ScanRepository
import com.safeshield.app.data.repository.SignatureRepository
import com.safeshield.app.data.repository.VirusTotalRepository
import com.safeshield.app.domain.model.ScanState
import com.safeshield.app.domain.model.ScanTarget
import com.safeshield.app.domain.model.ScanType
import com.safeshield.app.domain.model.Severity
import com.safeshield.app.domain.model.ThreatFinding
import com.safeshield.app.domain.model.ThreatSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * The actual detector. Fully functional with no network:
 *   1. SHA-256 of every APK vs the bundled signature database,
 *   2. behavioural heuristics (permission combos, hidden icon, sideload, …),
 *   3. optional VirusTotal hash lookup when the user opted in.
 *
 * Targets are processed in parallel batches so a 300-app device finishes in
 * seconds, and progress is reported after every item so the UI never looks stuck.
 */
@Singleton
class ScanEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inventory: AppInventoryRepository,
    private val signatures: SignatureRepository,
    private val heuristics: HeuristicAnalyzer,
    private val virusTotal: VirusTotalRepository,
    private val scanRepository: ScanRepository,
    private val settings: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    data class Request(
        val type: ScanType,
        val customFolderUri: Uri? = null,
        val packageNames: List<String>? = null,  // realtime scan of a single app
        val parallelism: Int = DEFAULT_PARALLELISM,
    )

    data class Result(
        val findings: List<ThreatFinding>,
        val itemsScanned: Int,
        val startedAt: Long,
        val finishedAt: Long,
        val onlineUsed: Boolean,
        val state: ScanState,
    )

    fun interface ProgressSink {
        /** Called from a worker coroutine after every scanned item. */
        suspend fun onProgress(scanned: Int, total: Int, threats: Int, currentItem: String)
    }

    /** Lets the caller implement Pause/Resume/Cancel without owning engine state. */
    fun interface FlowControl {
        /** Suspends while paused; throws CancellationException when cancelled. */
        suspend fun awaitResume()
    }

    suspend fun scan(
        request: Request,
        progress: ProgressSink = ProgressSink { _, _, _, _ -> },
        flowControl: FlowControl = FlowControl { },
    ): Result {
        val startedAt = System.currentTimeMillis()
        signatures.seedIfEmpty()

        val onlineEnabled = settings.current().onlineModeEnabled && virusTotal.isConfigured
        val whitelist = scanRepository.whitelistedPackages()
        val targets = collectTargets(request).filter { it.id !in whitelist }

        val scanned = AtomicInteger(0)
        val threatCount = AtomicInteger(0)
        val findings = java.util.Collections.synchronizedList(mutableListOf<ThreatFinding>())

        coroutineScope {
            targets.chunked(request.parallelism).forEach { batch ->
                coroutineContext.ensureActive()
                flowControl.awaitResume()
                batch.map { target ->
                    async(ioDispatcher) {
                        val finding = runCatching { inspect(target, onlineEnabled) }.getOrNull()
                        if (finding != null) {
                            findings += finding
                            threatCount.incrementAndGet()
                        }
                        progress.onProgress(
                            scanned.incrementAndGet(),
                            targets.size,
                            threatCount.get(),
                            target.label,
                        )
                    }
                }.awaitAll()
            }
        }

        val finishedAt = System.currentTimeMillis()
        return Result(
            findings = findings.sortedByDescending { it.severity.ordinal },
            itemsScanned = targets.size,
            startedAt = startedAt,
            finishedAt = finishedAt,
            onlineUsed = onlineEnabled,
            state = ScanState.COMPLETED,
        )
    }

    /** Returns a finding, or null when the target is clean. */
    suspend fun inspect(target: ScanTarget, onlineEnabled: Boolean): ThreatFinding? {
        val path = target.apkPath
        val hash = when {
            path == null -> null
            path.startsWith("content://") -> hashOfUri(Uri.parse(path))
            else -> File(path).takeIf { it.exists() && it.canRead() }?.let { Hashing.sha256(it) }
        }

        // 1) Signature match – strongest and instant.
        if (hash != null) {
            signatures.lookup(hash)?.let { sig ->
                return ThreatFinding(
                    targetId = target.id,
                    label = target.label,
                    packageName = target.id.takeIf { target.isInstalledApp },
                    filePath = target.apkPath,
                    sha256 = hash,
                    severity = sig.severity,
                    source = ThreatSource.SIGNATURE,
                    malwareName = sig.malwareName,
                    reasons = listOf("Matches a known malware fingerprint (${sig.malwareName})."),
                )
            }
        }

        // 2) Heuristics – only meaningful for installed packages.
        var heuristicFinding: ThreatFinding? = null
        if (target.isInstalledApp) {
            val info = inventory.packageInfoOf(target.id)
            val appInfo = info?.applicationInfo
            if (info != null && appInfo != null) {
                val verdict = heuristics.analyze(
                    HeuristicAnalyzer.Candidate(
                        permissions = info.requestedPermissions?.toSet().orEmpty(),
                        installedFromUnknownSource = !inventory.isFromTrustedInstaller(target.id),
                        hasLauncherIcon = inventory.hasLauncherIcon(target.id),
                        isDeviceAdmin = inventory.isDeviceAdmin(target.id),
                        isAccessibilityService = inventory.declaresAccessibilityService(info),
                        isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                        isDebuggable = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0,
                        targetSdk = appInfo.targetSdkVersion,
                    )
                )
                if (verdict.severity != Severity.CLEAN) {
                    heuristicFinding = ThreatFinding(
                        targetId = target.id,
                        label = target.label,
                        packageName = target.id,
                        filePath = target.apkPath,
                        sha256 = hash,
                        severity = verdict.severity,
                        source = ThreatSource.HEURISTIC,
                        malwareName = null,
                        reasons = verdict.reasons,
                    )
                }
            }
        }

        // 3) VirusTotal – hash only, and only if the user enabled it.
        if (onlineEnabled && hash != null) {
            val vt = virusTotal.lookup(hash)
            if (vt != null && vt.severity != Severity.CLEAN) {
                val merged = buildList {
                    add("${vt.malicious} security vendors flagged this file.")
                    heuristicFinding?.reasons?.let(::addAll)
                }
                return ThreatFinding(
                    targetId = target.id,
                    label = target.label,
                    packageName = target.id.takeIf { target.isInstalledApp },
                    filePath = target.apkPath,
                    sha256 = hash,
                    severity = maxOf(vt.severity, heuristicFinding?.severity ?: Severity.CLEAN),
                    source = ThreatSource.VIRUSTOTAL,
                    malwareName = vt.label,
                    reasons = merged,
                )
            }
        }

        return heuristicFinding
    }

    suspend fun collectTargets(request: Request): List<ScanTarget> = withContext(ioDispatcher) {
        when (request.type) {
            ScanType.QUICK -> inventory.scanTargets(includeSystem = false)
            ScanType.FULL -> inventory.scanTargets(includeSystem = true) + storageApks()
            ScanType.CUSTOM -> request.customFolderUri?.let { folderApks(it) }.orEmpty()
            ScanType.REALTIME -> request.packageNames.orEmpty().mapNotNull { pkg ->
                val path = inventory.apkPathOf(pkg) ?: return@mapNotNull null
                ScanTarget(pkg, inventory.labelOf(pkg), path, true, File(path).length())
            }
        }
    }

    /**
     * Legacy-friendly sweep of the public Download/app directories.
     * On API 30+ we only look at locations this app can legally read without
     * MANAGE_EXTERNAL_STORAGE; anything else the user picks via Custom Scan.
     */
    private fun storageApks(): List<ScanTarget> {
        val roots = buildList {
            runCatching {
                add(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS))
            }
            addAll(context.getExternalFilesDirs(null).filterNotNull())
            add(context.cacheDir)
        }
        val out = mutableListOf<ScanTarget>()
        roots.filterNotNull().forEach { root ->
            runCatching {
                root.walkTopDown()
                    .maxDepth(4)
                    .filter { it.isFile && it.extension.equals("apk", true) && it.canRead() }
                    .take(MAX_FILES_PER_ROOT)
                    .forEach { f ->
                        out += ScanTarget(f.absolutePath, f.name, f.absolutePath, false, f.length())
                    }
            }
        }
        return out.distinctBy { it.id }
    }

    /** Custom scan over a SAF tree the user explicitly granted. */
    private fun folderApks(treeUri: Uri): List<ScanTarget> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val out = mutableListOf<ScanTarget>()
        fun walk(dir: DocumentFile, depth: Int) {
            if (depth > 5 || out.size >= MAX_FILES_PER_ROOT) return
            dir.listFiles().forEach { child ->
                when {
                    child.isDirectory -> walk(child, depth + 1)
                    child.name?.endsWith(".apk", true) == true ->
                        out += ScanTarget(
                            id = child.uri.toString(),
                            label = child.name ?: "APK",
                            apkPath = resolveToCache(child),
                            isInstalledApp = false,
                            sizeBytes = child.length(),
                        )
                }
            }
        }
        walk(root, 0)
        return out
    }

    /**
     * SAF gives us a Uri, but hashing wants a stream. We hash directly from the
     * content resolver and write nothing to disk.
     */
    private fun resolveToCache(file: DocumentFile): String? = file.uri.toString()

    suspend fun hashOfUri(uri: Uri): String? = withContext(ioDispatcher) {
        runCatching { context.contentResolver.openInputStream(uri)?.use { Hashing.sha256(it) } }.getOrNull()
    }

    companion object {
        const val DEFAULT_PARALLELISM = 6
        private const val MAX_FILES_PER_ROOT = 2000
    }
}
