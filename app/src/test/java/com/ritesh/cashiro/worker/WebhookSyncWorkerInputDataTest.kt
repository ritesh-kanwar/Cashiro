package com.ritesh.cashiro.worker

import com.ritesh.cashiro.data.webhook.WebhookSyncReason
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The reason + sendTest flag round-trip through `Data` is the only contract between
 * [com.ritesh.cashiro.data.webhook.WebhookSyncScheduler] (which builds the input) and
 * [WebhookSyncWorker] (which reads it). A renamed key would silently degrade every scheduled
 * sync to "INTERVAL, no test payload" — these tests pin the keys.
 */
class WebhookSyncWorkerInputDataTest {

    @Test
    fun `MANUAL reason round trips through worker input data`() {
        val data = WebhookSyncWorker.inputData(WebhookSyncReason.MANUAL, sendTestPayload = false)

        assertEquals(WebhookSyncReason.MANUAL.name, data.getString("reason"))
        assertEquals(false, data.getBoolean("send_test", true))
    }

    @Test
    fun `TEST reason carries sendTestPayload flag`() {
        val data = WebhookSyncWorker.inputData(WebhookSyncReason.TEST, sendTestPayload = true)

        assertEquals(WebhookSyncReason.TEST.name, data.getString("reason"))
        assertEquals(true, data.getBoolean("send_test", false))
    }

    @Test
    fun `every WebhookSyncReason serialises by name`() {
        // Catches the case where someone adds a new reason value but the worker's reader
        // (WebhookSyncReason.valueOf) still works — the input data is the contract.
        for (reason in WebhookSyncReason.entries) {
            val data = WebhookSyncWorker.inputData(reason, sendTestPayload = false)
            assertEquals(reason.name, data.getString("reason"))
        }
    }
}
