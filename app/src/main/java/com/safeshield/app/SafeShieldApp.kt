package com.safeshield.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.data.repository.BillingRepository
import com.safeshield.app.data.repository.SignatureRepository
import com.safeshield.app.worker.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SafeShieldApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var signatures: SignatureRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var scheduler: WorkScheduler
    @Inject lateinit var billing: BillingRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // First launch: load the bundled offline signature database so the
            // very first scan works with zero network access.
            runCatching { signatures.seedIfEmpty() }

            val prefs = settings.current()
            scheduler.applySchedule(prefs.schedule)
            scheduler.scheduleSignatureUpdates()
            if (prefs.realtimeProtectionEnabled) scheduler.scheduleApkWatch()
        }
        billing.connect()
    }
}
