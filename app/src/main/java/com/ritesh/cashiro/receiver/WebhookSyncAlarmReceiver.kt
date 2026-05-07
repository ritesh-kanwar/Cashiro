package com.ritesh.cashiro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ritesh.cashiro.data.webhook.WebhookSyncReason
import com.ritesh.cashiro.data.webhook.WebhookSyncScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WebhookSyncAlarmReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun webhookSyncScheduler(): WebhookSyncScheduler
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java
        )
        val scheduler = entryPoint.webhookSyncScheduler()
        scheduler.enqueueImmediate(WebhookSyncReason.SCHEDULED)
        // applyScheduling() re-arms the next alarm. BroadcastReceiver only has ~10s of guaranteed
        // process time; without goAsync() the OS could kill us before that completes and the
        // schedule would silently stop firing.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                scheduler.applyScheduling()
            } catch (t: Throwable) {
                Log.e("WebhookSyncAlarm", "applyScheduling failed after alarm", t)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
