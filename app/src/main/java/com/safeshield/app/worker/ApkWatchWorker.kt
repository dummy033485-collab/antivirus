package com.safeshield.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safeshield.app.domain.engine.ScanEngine
import com.safeshield.app.domain.model.ScanTarget
import com.safeshield.app.service.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

/**
 * Periodic sweep of the Downloads folder for APKs that appeared since the last
 * run. Paired with [com.safeshield.app.service.DownloadsObserver] which reacts
 * instantly while the app process is alive.
 */
@HiltWorker
class ApkWatchWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val engine: ScanEngine,
    private val notifications: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val since = inputData.getLong(KEY_SINCE, System.currentTimeMillis() - DEFAULT_WINDOW)
        val downloads = android.os.Environment
            .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)

        val fresh = runCatching {
            downloads.listFiles { f -> f.isFile && f.extension.equals("apk", true) && f.lastModified() >= since }
                ?.toList().orEmpty()
        }.getOrDefault(emptyList<File>())

        fresh.forEach { file ->
            val target = ScanTarget(file.absolutePath, file.name, file.absolutePath, false, file.length())
            val finding = engine.inspect(target, onlineEnabled = false)
            if (finding != null) notifications.showInstallVerdict(file.name, true)
        }
        Result.success()
    }.getOrElse { Result.success() } // never retry-storm on storage permission issues

    companion object {
        const val KEY_SINCE = "since"
        private const val DEFAULT_WINDOW = 6 * 60 * 60 * 1000L
    }
}
