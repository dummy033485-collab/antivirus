package com.safeshield.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.safeshield.app.R
import com.safeshield.app.core.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keeps the scan alive while the app is backgrounded. It holds no scan logic –
 * [ScanController] owns that – so rotating or leaving the app never restarts work.
 */
@AndroidEntryPoint
class ScanForegroundService : Service() {

    @Inject lateinit var notifications: NotificationHelper

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notifications.buildScanNotification(
            getString(R.string.scanning), 0, 0,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                Constants.NOTIF_ID_SCAN,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(Constants.NOTIF_ID_SCAN, notification)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        notifications.cancelScanNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
