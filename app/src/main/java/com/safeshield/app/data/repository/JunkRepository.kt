package com.safeshield.app.data.repository

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.os.storage.StorageManager
import com.safeshield.app.domain.model.JunkItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scoped-storage honest junk cleaner.
 *
 * Android 8+ forbids one app from deleting another app's cache, so SafeShield
 * does exactly two legal things:
 *   • deletes its OWN cache and temp files,
 *   • reports other apps' cache sizes (requires PACKAGE_USAGE_STATS) and opens
 *     the system storage screen so the user clears them themselves.
 * No root, no fake "1.2 GB cleaned" numbers.
 */
@Singleton
class JunkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inventory: AppInventoryRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(android.app.AppOpsManager::class.java) ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    suspend fun findJunk(): List<JunkItem> = withContext(ioDispatcher) {
        buildList {
            addAll(ownTempFiles())
            if (hasUsageStatsPermission()) addAll(otherAppCaches())
        }.sortedByDescending { it.sizeBytes }
    }

    private fun ownTempFiles(): List<JunkItem> {
        val dirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
        return dirs.map { dir ->
            JunkItem(
                packageName = context.packageName,
                label = "SafeShield temporary files",
                path = dir.absolutePath,
                sizeBytes = dir.sizeRecursive(),
                deletableByApp = true,
            )
        }.filter { it.sizeBytes > 0 }
    }

    private suspend fun otherAppCaches(): List<JunkItem> {
        val storageStats = context.getSystemService(StorageStatsManager::class.java) ?: return emptyList()
        val uuid = StorageManager.UUID_DEFAULT
        return inventory.installedPackages(includeSystem = false).mapNotNull { info ->
            val app = info.applicationInfo ?: return@mapNotNull null
            val stats = runCatching {
                storageStats.queryStatsForUid(uuid, app.uid)
            }.getOrNull() ?: return@mapNotNull null
            if (stats.cacheBytes <= 0) return@mapNotNull null
            JunkItem(
                packageName = info.packageName,
                label = app.loadLabel(context.packageManager).toString(),
                path = null,
                sizeBytes = stats.cacheBytes,
                deletableByApp = false,
            )
        }
    }

    /** Only ever removes files owned by SafeShield itself. */
    suspend fun cleanOwnJunk(): Long = withContext(ioDispatcher) {
        var freed = 0L
        listOfNotNull(context.cacheDir, context.externalCacheDir).forEach { dir ->
            freed += dir.sizeRecursive()
            runCatching { dir.listFiles()?.forEach { it.deleteRecursively() } }
        }
        freed
    }

    fun openStorageSettings(packageName: String?) {
        val intent = if (packageName != null) {
            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.fromParts("package", packageName, null))
        } else {
            android.content.Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        }.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openUsageAccessSettings() {
        runCatching {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun File.sizeRecursive(): Long = runCatching {
        walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }.getOrDefault(0L)

    @Suppress("unused")
    private fun unusedPmReference(pm: PackageManager) = Unit
}
