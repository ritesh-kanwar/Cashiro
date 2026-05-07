package com.ritesh.cashiro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ritesh.cashiro.data.manager.NotificationScheduler
import com.ritesh.cashiro.data.webhook.WebhookSyncScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receiver that reschedules alarms after device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun notificationScheduler(): NotificationScheduler
        fun webhookSyncScheduler(): WebhookSyncScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "Device rebooted, rescheduling alarms")

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootReceiverEntryPoint::class.java
        )
        val scheduler = entryPoint.notificationScheduler()
        val webhookSyncScheduler = entryPoint.webhookSyncScheduler()

        // BroadcastReceiver onReceive has only ~10s of guaranteed lifetime. Without goAsync()
        // the OS can kill the process before applyScheduling()/scheduleDailyReminder() finish
        // and alarms would never be restored after a reboot.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Each scheduler in its own try/catch so a failure in one (e.g. notification
                // channels missing on a fresh boot) doesn't prevent the other from re-arming.
                // Explicit catch lets CancellationException propagate to keep structured
                // concurrency intact if anything ever wires this scope to a parent.
                try {
                    scheduler.scheduleDailyReminder()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to reschedule daily reminder", t)
                }
                try {
                    webhookSyncScheduler.applyScheduling()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to re-apply webhook scheduling", t)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
