package com.safeshield.app.data.repository

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import com.safeshield.app.domain.engine.RiskScorer
import com.safeshield.app.domain.model.AppRiskInfo
import com.safeshield.app.domain.model.ScanTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for "what is installed on this device".
 * All PackageManager access is funnelled through here so the rest of the app
 * stays framework-free and testable.
 */
@Singleton
class AppInventoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val riskScorer: RiskScorer,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val pm: PackageManager get() = context.packageManager

    private val trustedInstallers = setOf(
        "com.android.vending",         // Google Play
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.samsung.android.app.galaxyapps",
        "com.huawei.appmarket",
        "com.amazon.venezia",
    )

    suspend fun installedPackages(includeSystem: Boolean = true): List<PackageInfo> =
        withContext(ioDispatcher) {
            val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES
            val all = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledPackages(flags)
                }
            }.getOrDefault(emptyList())

            all.filter { info ->
                val app = info.applicationInfo ?: return@filter false
                includeSystem || !app.isSystem()
            }
        }

    suspend fun scanTargets(includeSystem: Boolean = true): List<ScanTarget> =
        installedPackages(includeSystem).mapNotNull { info ->
            val app = info.applicationInfo ?: return@mapNotNull null
            val path = app.sourceDir ?: return@mapNotNull null
            ScanTarget(
                id = info.packageName,
                label = app.loadLabel(pm).toString(),
                apkPath = path,
                isInstalledApp = true,
                sizeBytes = runCatching { File(path).length() }.getOrDefault(0L),
            )
        }

    suspend fun riskInfo(whitelisted: Set<String> = emptySet(), includeSystem: Boolean = false): List<AppRiskInfo> =
        withContext(ioDispatcher) {
            installedPackages(includeSystem).mapNotNull { info ->
                val app = info.applicationInfo ?: return@mapNotNull null
                val permissions = info.requestedPermissions?.toSet().orEmpty()
                val unknownSource = !isFromTrustedInstaller(info.packageName)
                val hasLauncher = hasLauncherIcon(info.packageName)
                val deviceAdmin = isDeviceAdmin(info.packageName)
                val accessibility = declaresAccessibilityService(info)
                val system = app.isSystem()

                val result = riskScorer.score(
                    RiskScorer.Input(
                        permissions = permissions,
                        installedFromUnknownSource = unknownSource,
                        hasLauncherIcon = hasLauncher,
                        isDeviceAdmin = deviceAdmin,
                        isAccessibilityService = accessibility,
                        isSystemApp = system,
                    )
                )

                AppRiskInfo(
                    packageName = info.packageName,
                    label = app.loadLabel(pm).toString(),
                    versionName = info.versionName ?: "—",
                    installerPackage = installerOf(info.packageName),
                    isSystemApp = system,
                    installedAt = info.firstInstallTime,
                    riskScore = result.score,
                    riskLevel = result.level,
                    reasonResIds = result.reasonResIds,
                    dangerousPermissions = result.dangerousPermissions,
                    hasLauncherIcon = hasLauncher,
                    isDeviceAdmin = deviceAdmin,
                    isAccessibilityService = accessibility,
                    isWhitelisted = info.packageName in whitelisted,
                )
            }.sortedByDescending { it.riskScore }
        }

    fun installerOf(packageName: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION") pm.getInstallerPackageName(packageName)
        }
    }.getOrNull()

    fun isFromTrustedInstaller(packageName: String): Boolean =
        installerOf(packageName) in trustedInstallers

    fun hasLauncherIcon(packageName: String): Boolean =
        runCatching { pm.getLaunchIntentForPackage(packageName) != null }.getOrDefault(true)

    fun isDeviceAdmin(packageName: String): Boolean = runCatching {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        dpm.activeAdmins?.any { it.packageName == packageName } == true
    }.getOrDefault(false)

    fun declaresAccessibilityService(info: PackageInfo): Boolean =
        info.services?.any { it.isAccessibility() } == true

    fun labelOf(packageName: String): String = runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    fun apkPathOf(packageName: String): String? = runCatching {
        pm.getApplicationInfo(packageName, 0).sourceDir
    }.getOrNull()

    fun packageInfoOf(packageName: String): PackageInfo? = runCatching {
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION") pm.getPackageInfo(packageName, flags)
        }
    }.getOrNull()

    /** Launches the system "App info" screen. */
    fun openAppSettings(packageName: String) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Asks the OS to uninstall; the user always confirms in the system dialog. */
    fun requestUninstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
            .setData(android.net.Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun ApplicationInfo.isSystem(): Boolean =
        (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0

    private fun ServiceInfo.isAccessibility(): Boolean =
        permission == "android.permission.BIND_ACCESSIBILITY_SERVICE"

    fun componentEnabled(component: ComponentName): Boolean = runCatching {
        pm.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }.getOrDefault(true)
}
