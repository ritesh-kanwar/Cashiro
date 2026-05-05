package com.ritesh.cashiro.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.ritesh.cashiro.data.webhook.WebhookSyncManager
import com.ritesh.cashiro.data.webhook.WebhookSyncReason
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WebhookSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val webhookSyncManager: WebhookSyncManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val reason = runCatching {
            WebhookSyncReason.valueOf(inputData.getString(KEY_REASON) ?: WebhookSyncReason.INTERVAL.name)
        }.getOrDefault(WebhookSyncReason.INTERVAL)
        val sendTest = inputData.getBoolean(KEY_SEND_TEST, false)
        val result = webhookSyncManager.syncAll(reason = reason, sendTestPayload = sendTest)
        return if (result.anyRetryableFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val KEY_REASON = "reason"
        private const val KEY_SEND_TEST = "send_test"

        fun inputData(reason: WebhookSyncReason, sendTestPayload: Boolean): Data {
            return Data.Builder()
                .putString(KEY_REASON, reason.name)
                .putBoolean(KEY_SEND_TEST, sendTestPayload)
                .build()
        }
    }
}
