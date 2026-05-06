package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.data.webhook.WebhookHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The webhook_profiles.headers_json column is consumed by both runtime reads (decodeHeaders) and
 * the 50 -> 51 migration that aggregates rows from the soon-to-be-dropped webhook_profile_headers
 * table into the same JSON shape. Pinning the round-trip protects users who upgrade past either
 * boundary from losing custom-header configuration.
 */
class WebhookHeaderEncodingTest {

    @Test
    fun `headers round trip through encode and decode`() {
        val headers = listOf(
            WebhookHeader(key = "X-Auth-Token", value = "secret"),
            WebhookHeader(key = "X-Trace-Id", value = "abc-123")
        )

        val encoded = WebhookHeaderEncoder.encode(headers)
        val decoded = WebhookHeaderEncoder.decode(encoded)

        assertEquals(headers, decoded)
    }

    @Test
    fun `decode tolerates an empty list`() {
        assertEquals(emptyList<WebhookHeader>(), WebhookHeaderEncoder.decode("[]"))
    }

    @Test
    fun `decode falls back to empty list on malformed input`() {
        // Defensive: a corrupted row from a partial migration must not crash the app.
        assertEquals(emptyList<WebhookHeader>(), WebhookHeaderEncoder.decode("not-json"))
        assertEquals(emptyList<WebhookHeader>(), WebhookHeaderEncoder.decode(""))
    }

    @Test
    fun `encode skips entries with a blank key`() {
        // Editor lets users leave half-typed rows; saving them as headers makes no sense.
        val headers = listOf(
            WebhookHeader(key = "X-Auth", value = "abc"),
            WebhookHeader(key = "", value = "stray")
        )
        val encoded = WebhookHeaderEncoder.encode(headers)
        val decoded = WebhookHeaderEncoder.decode(encoded)
        assertEquals(1, decoded.size)
        assertEquals("X-Auth", decoded.single().key)
    }

    @Test
    fun `encoded shape uses the documented JSON array of {key, value} objects`() {
        // The migration emits this shape from raw SQL; if encode drifts, migrated data won't
        // decode cleanly. Worth pinning the surface format itself.
        val encoded = WebhookHeaderEncoder.encode(
            listOf(WebhookHeader(key = "K", value = "V"))
        )
        assertTrue("expected JSON array", encoded.startsWith("[") && encoded.endsWith("]"))
        assertTrue("expected key field", encoded.contains("\"key\":\"K\""))
        assertTrue("expected value field", encoded.contains("\"value\":\"V\""))
    }
}
