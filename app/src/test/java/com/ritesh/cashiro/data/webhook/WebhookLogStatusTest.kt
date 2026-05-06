package com.ritesh.cashiro.data.webhook

import com.ritesh.cashiro.data.database.entity.WebhookLogStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class WebhookLogStatusTest {

    @Test
    fun `legacy DELIVERED maps to SUCCESS`() {
        assertEquals(WebhookLogStatus.SUCCESS, WebhookLogStatus.fromLegacy("DELIVERED"))
    }

    @Test
    fun `legacy SUCCESS string maps to SUCCESS`() {
        assertEquals(WebhookLogStatus.SUCCESS, WebhookLogStatus.fromLegacy("SUCCESS"))
        assertEquals(WebhookLogStatus.SUCCESS, WebhookLogStatus.fromLegacy("success"))
    }

    @Test
    fun `legacy FAILED and ERROR map to FAILURE`() {
        assertEquals(WebhookLogStatus.FAILURE, WebhookLogStatus.fromLegacy("FAILED"))
        assertEquals(WebhookLogStatus.FAILURE, WebhookLogStatus.fromLegacy("ERROR"))
        assertEquals(WebhookLogStatus.FAILURE, WebhookLogStatus.fromLegacy("error"))
    }

    @Test
    fun `unknown legacy value falls back to FAILURE`() {
        // Conservative: an unrecognised status is more useful as 'something went wrong' than as 'success'.
        assertEquals(WebhookLogStatus.FAILURE, WebhookLogStatus.fromLegacy("WHATEVER"))
        assertEquals(WebhookLogStatus.FAILURE, WebhookLogStatus.fromLegacy(""))
    }
}
