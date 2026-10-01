package com.safeshield.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.safeshield.app.R
import com.safeshield.app.core.Constants
import com.safeshield.app.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    init { createChannels() }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val scan = NotificationChannel(
            Constants.CHANNEL_SCAN,
            context.getString(R.string.notif_channel_scan),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }

        val threat = NotificationChannel(
            Constants.CHANNEL_THREAT,
            context.getString(R.string.notif_channel_threat),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { enableVibration(true) }

        manager.createNotificationChannels(listOf(scan, threat))
    }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun buildScanNotification(text: String, progress: Int, max: Int) =
        NotificationCompat.Builder(context, Constants.CHANNEL_SCAN)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.notif_scanning_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(max.coerceAtLeast(1), progress, max <= 0)
            .setContentIntent(openAppIntent())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    fun updateScanProgress(scanned: Int, total: Int, item: String) {
        if (!hasPermission()) return
        runCatching {
            manager.notify(
                Constants.NOTIF_ID_SCAN,
                buildScanNotification(item, scanned, total),
            )
        }
    }

    fun showThreatSummary(count: Int, firstLabel: String) {
        if (!hasPermission()) return
        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_THREAT)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.notif_threat_title))
            .setContentText(context.getString(R.string.notif_threat_text, firstLabel))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(R.string.threats_found, count) + " · " +
                        context.getString(R.string.notif_threat_text, firstLabel)
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        runCatching { manager.notify(Constants.NOTIF_ID_THREAT_BASE, notification) }
    }

    fun showInstallVerdict(label: String, threatening: Boolean) {
        if (!hasPermission()) return
        val builder = if (threatening) {
            NotificationCompat.Builder(context, Constants.CHANNEL_THREAT)
                .setContentTitle(context.getString(R.string.notif_threat_title))
                .setContentText(context.getString(R.string.notif_threat_text, label))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        } else {
            NotificationCompat.Builder(context, Constants.CHANNEL_SCAN)
                .setContentTitle(context.getString(R.string.notif_clean_install_title))
                .setContentText(context.getString(R.string.notif_clean_install_text, label))
                .setPriority(NotificationCompat.PRIORITY_LOW)
        }
        val notification = builder
            .setSmallIcon(R.drawable.ic_shield)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        runCatching {
            manager.notify(Constants.NOTIF_ID_THREAT_BASE + label.hashCode().and(0xFFF), notification)
        }
    }

    fun cancelScanNotification() = manager.cancel(Constants.NOTIF_ID_SCAN)

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
