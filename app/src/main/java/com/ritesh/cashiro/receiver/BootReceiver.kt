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

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device rebooted, rescheduling alarms")

            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java
            )
            val scheduler = entryPoint.notificationScheduler()
            val webhookSyncScheduler = entryPoint.webhookSyncScheduler()

            receiverScope.launch {
                try {
                    scheduler.scheduleDailyReminder()
                    webhookSyncScheduler.applyScheduling()
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms after boot", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
