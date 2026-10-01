package com.safeshield.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.repository.AppInventoryRepository
import com.safeshield.app.domain.model.ScanType
import com.safeshield.app.service.NotificationHelper
import com.safeshield.app.service.ScanController
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Real-time protection: scans one freshly installed/updated package. */
@HiltWorker
class PackageScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scanController: ScanController,
    private val inventory: AppInventoryRepository,
    private val settings: SettingsRepository,
    private val notifications: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pkg = inputData.getString(KEY_PACKAGE) ?: return Result.failure()
        if (!settings.current().realtimeProtectionEnabled) return Result.success()

        return runCatching {
            val findings = scanController.scanSilently(ScanType.REALTIME, listOf(pkg))
            val label = inventory.labelOf(pkg)
            notifications.showInstallVerdict(label, findings.isNotEmpty())
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val KEY_PACKAGE = "package_name"
    }
}
