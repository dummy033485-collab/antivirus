package com.safeshield.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.safeshield.app.worker.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Real-time protection entry point. Keeps work off the main thread by handing
 * the package straight to WorkManager.
 */
@AndroidEntryPoint
class PackageEventReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: WorkScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_PACKAGE_ADDED && action != Intent.ACTION_PACKAGE_REPLACED) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return

        // PACKAGE_ADDED with EXTRA_REPLACING is the remove half of an update.
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) &&
            action == Intent.ACTION_PACKAGE_ADDED
        ) return

        scheduler.scanPackageNow(packageName)
    }
}
