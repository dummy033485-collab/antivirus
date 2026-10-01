package com.safeshield.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.safeshield.app.core.Constants
import com.safeshield.app.data.prefs.ScanSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun applySchedule(schedule: ScanSchedule) {
        if (schedule == ScanSchedule.OFF) {
            workManager.cancelUniqueWork(Constants.WORK_SCHEDULED_SCAN)
            return
        }
        val interval = if (schedule == ScanSchedule.DAILY) 1L else 7L
        val request = PeriodicWorkRequestBuilder<ScheduledScanWorker>(interval, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            Constants.WORK_SCHEDULED_SCAN,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun scheduleSignatureUpdates() {
        val request = PeriodicWorkRequestBuilder<SignatureUpdateWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            Constants.WORK_SIGNATURE_UPDATE,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleApkWatch() {
        val request = PeriodicWorkRequestBuilder<ApkWatchWorker>(6, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            Constants.WORK_APK_WATCH,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun scanPackageNow(packageName: String) {
        val request = OneTimeWorkRequestBuilder<PackageScanWorker>()
            .setInputData(Data.Builder().putString(PackageScanWorker.KEY_PACKAGE, packageName).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            "scan_$packageName",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun updateSignaturesNow() {
        workManager.enqueue(OneTimeWorkRequestBuilder<SignatureUpdateWorker>().build())
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(Constants.WORK_SCHEDULED_SCAN)
        workManager.cancelUniqueWork(Constants.WORK_SIGNATURE_UPDATE)
        workManager.cancelUniqueWork(Constants.WORK_APK_WATCH)
    }
}
