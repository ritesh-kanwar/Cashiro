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
    fun `encoded shape decodes structurally to a list of key-value entries`() {
        // The migration emits the same shape from raw SQL; if encode drifts, migrated data won't
        // decode cleanly. Decode-and-assert avoids whitespace-sensitive substring matching that
        // would silently pass if the serializer ever inserts spaces around the colon.
        val original = listOf(WebhookHeader(key = "K", value = "V"))
        val encoded = WebhookHeaderEncoder.encode(original)
        assertTrue("expected JSON array", encoded.startsWith("[") && encoded.endsWith("]"))
        assertEquals(original, WebhookHeaderEncoder.decode(encoded))
    }

    @Test
    fun `sanitizeForExport wipes values but keeps keys`() {
        // Pins the BackupExporter contract: a shared backup file must never carry bearer tokens
        // or API keys, but the user must be able to see which header keys to re-enter on restore.
        val saved = listOf(
            WebhookHeader(key = "Authorization", value = "Bearer secret-123"),
            WebhookHeader(key = "X-Api-Key", value = "extremely-private")
        )

        val sanitized = WebhookHeaderEncoder.sanitizeForExport(saved)

        assertEquals(2, sanitized.size)
        assertEquals("Authorization", sanitized[0].key)
        assertEquals("X-Api-Key", sanitized[1].key)
        sanitized.forEach { assertEquals("", it.value) }
    }

    @Test
    fun `sanitizeForExport on empty list returns empty list without throwing`() {
        assertEquals(emptyList<WebhookHeader>(), WebhookHeaderEncoder.sanitizeForExport(emptyList()))
    }
}
