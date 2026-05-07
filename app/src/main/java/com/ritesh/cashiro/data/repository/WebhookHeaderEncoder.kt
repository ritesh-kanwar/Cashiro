package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.data.webhook.WebhookHeader
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Single source of truth for the JSON shape stored in webhook_profiles.headers_json. Both the
 * runtime repository and the 50 -> 51 schema migration go through these helpers so a row written
 * by either path decodes identically.
 */
object WebhookHeaderEncoder {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(headers: List<WebhookHeader>): String =
        json.encodeToString(headers.filter { it.key.isNotBlank() })

    fun decode(headersJson: String): List<WebhookHeader> = runCatching {
        json.decodeFromString<List<WebhookHeader>>(headersJson)
    }.getOrDefault(emptyList())

    /**
     * Returns a copy of [headers] with every value wiped to "". Used when exporting backups so a
     * shared file never leaks bearer tokens / API keys; keys are preserved so a restore can prompt
     * the user to re-enter the values.
     */
    fun sanitizeForExport(headers: List<WebhookHeader>): List<WebhookHeader> =
        headers.map { it.copy(value = "") }
}
