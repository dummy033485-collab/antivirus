package com.safeshield.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.safeshield.app.data.prefs.SettingsRepository
import com.safeshield.app.worker.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Re-arms scheduled work after a reboot or an app update. */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: WorkScheduler
    @Inject lateinit var settings: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = settings.current()
                scheduler.applySchedule(prefs.schedule)
                scheduler.scheduleSignatureUpdates()
                if (prefs.realtimeProtectionEnabled) scheduler.scheduleApkWatch()
            } finally {
                pending.finish()
            }
        }
    }
}
