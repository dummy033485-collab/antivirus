package com.safeshield.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safeshield.app.domain.model.ScanType
import com.safeshield.app.service.NotificationHelper
import com.safeshield.app.service.ScanController
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScheduledScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scanController: ScanController,
    private val notifications: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val findings = scanController.scanSilently(ScanType.QUICK)
        if (findings.isNotEmpty()) {
            notifications.showThreatSummary(findings.size, findings.first().label)
        }
        Result.success()
    }.getOrElse { Result.retry() }
}
