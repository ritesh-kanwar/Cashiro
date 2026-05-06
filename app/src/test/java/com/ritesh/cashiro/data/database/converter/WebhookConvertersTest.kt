package com.ritesh.cashiro.data.database.converter

import com.ritesh.cashiro.data.database.entity.WebhookDataType
import com.ritesh.cashiro.data.database.entity.WebhookLogStatus
import com.ritesh.cashiro.data.database.entity.WebhookRangePreset
import com.ritesh.cashiro.data.webhook.WebhookSyncReason
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Round-trip tests for the webhook enum TypeConverters. Each converter is the only thing standing
 * between persisted SQLite TEXT and the typed enum the rest of the code expects, so a regression
 * here corrupts user data on read.
 */
class WebhookConvertersTest {

    private val converters = Converters()

    @Test
    fun `WebhookLogStatus round trips through converter`() {
        for (status in WebhookLogStatus.entries) {
            val text = converters.fromWebhookLogStatus(status)
            assertEquals(status, converters.toWebhookLogStatus(text))
        }
    }

    @Test
    fun `WebhookLogStatus converter accepts legacy strings written before the migration`() {
        // Defensive: if a legacy "DELIVERED" / "ERROR" row survives the migration, reads still work.
        assertEquals(WebhookLogStatus.SUCCESS, converters.toWebhookLogStatus("DELIVERED"))
        assertEquals(WebhookLogStatus.FAILURE, converters.toWebhookLogStatus("ERROR"))
        assertEquals(WebhookLogStatus.FAILURE, converters.toWebhookLogStatus("FAILED"))
    }

    @Test
    fun `WebhookDataType round trips`() {
        for (value in WebhookDataType.entries) {
            assertEquals(value, converters.toWebhookDataType(converters.fromWebhookDataType(value)))
        }
    }

    @Test
    fun `WebhookRangePreset round trips`() {
        for (value in WebhookRangePreset.entries) {
            assertEquals(value, converters.toWebhookRangePreset(converters.fromWebhookRangePreset(value)))
        }
    }

    @Test
    fun `WebhookSyncReason round trips`() {
        for (value in WebhookSyncReason.entries) {
            assertEquals(value, converters.toWebhookSyncReason(converters.fromWebhookSyncReason(value)))
        }
    }

    @Test
    fun `unknown enum values fall back to safe defaults`() {
        // The DB could outlive a release that removed an enum entry; reads must not crash.
        assertEquals(WebhookDataType.SUMMARY, converters.toWebhookDataType("REMOVED_VALUE"))
        assertEquals(WebhookRangePreset.SINCE_LAST_SUCCESS, converters.toWebhookRangePreset("REMOVED"))
        assertEquals(WebhookSyncReason.MANUAL, converters.toWebhookSyncReason("REMOVED"))
    }
}
