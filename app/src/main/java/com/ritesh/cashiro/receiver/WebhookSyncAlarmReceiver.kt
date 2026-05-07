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
        // BroadcastReceiver only has ~10s of guaranteed process time, and any work done before
        // goAsync() blocks the receiver thread. Acquire the PendingResult first so the receiver
        // returns immediately, then run the scheduler work inside a coroutine. enqueueImmediate
        // throwing must not skip applyScheduling — they're isolated with their own runCatching
        // so one failure can't silently disarm the next alarm.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                runCatching { scheduler.enqueueImmediate(WebhookSyncReason.SCHEDULED) }
                    .onFailure { Log.e("WebhookSyncAlarm", "enqueueImmediate failed", it) }
                runCatching { scheduler.applyScheduling() }
                    .onFailure { Log.e("WebhookSyncAlarm", "applyScheduling failed after alarm", it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
