package com.ritesh.cashiro.data.webhook

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.receiver.WebhookSyncAlarmReceiver
import com.ritesh.cashiro.worker.WebhookSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class WebhookSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val workManager = WorkManager.getInstance(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val PERIODIC_WORK_NAME = "webhook_periodic_sync"
        const val ONE_TIME_WORK_NAME = "webhook_one_time_sync"
        private const val ALARM_REQUEST_CODE_LEGACY = 2911
        private const val ALARM_INTENT_EXTRA_ID = "webhook_schedule_id"
    }

    suspend fun applyScheduling() {
        val settings = userPreferencesRepository.webhookSettings.first()
        if (!userPreferencesRepository.isDeveloperModeEnabled.first()) {
            // Feature is gated behind developer mode. Tear down any work / alarms that may have
            // been scheduled while it was on so toggling it off immediately stops background sync.
            cancelPeriodic()
            cancelAllScheduledAlarms(settings.scheduledTimes.map { it.id })
            return
        }
        when (settings.syncMode) {
            WebhookSyncMode.INTERVAL -> {
                cancelAllScheduledAlarms(settings.scheduledTimes.map { it.id })
                schedulePeriodic(settings.intervalHours)
            }
            WebhookSyncMode.SCHEDULED -> {
                cancelPeriodic()
                cancelAllScheduledAlarms(settings.scheduledTimes.map { it.id })
                settings.scheduledTimes
                    .filter { it.enabled }
                    .forEach { scheduleSingleAlarm(it) }
            }
        }
    }

    fun enqueueImmediate(reason: WebhookSyncReason, sendTestPayload: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<WebhookSyncWorker>()
            .setConstraints(syncConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(WebhookSyncWorker.inputData(reason, sendTestPayload))
            .build()
        workManager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelPeriodic() {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /** Cancels the legacy single alarm plus alarms for the given list of IDs. */
    fun cancelAllScheduledAlarms(ids: List<String>) {
        // Legacy single-alarm cancel (pre-multi-time)
        val legacyIntent = Intent(context, WebhookSyncAlarmReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE_LEGACY,
            legacyIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { alarmManager.cancel(it) }

        ids.forEach { id ->
            val intent = Intent(context, WebhookSyncAlarmReceiver::class.java).apply {
                putExtra(ALARM_INTENT_EXTRA_ID, id)
            }
            PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { alarmManager.cancel(it) }
        }
    }

    private fun schedulePeriodic(intervalHours: Int) {
        val request = PeriodicWorkRequestBuilder<WebhookSyncWorker>(
            intervalHours.coerceAtLeast(1).toLong(),
            TimeUnit.HOURS
        )
            .setConstraints(syncConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun scheduleSingleAlarm(time: WebhookScheduledTime) {
        val nextRun = WebhookScheduleArithmetic.nextRunFor(time, LocalDateTime.now())
        val triggerAtMillis = nextRun.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, WebhookSyncAlarmReceiver::class.java).apply {
            putExtra(ALARM_INTENT_EXTRA_ID, time.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            time.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun syncConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}
