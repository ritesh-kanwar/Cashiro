package com.ritesh.cashiro.data.webhook

object WebhookValidation {

    const val MIN_INTERVAL_HOURS: Int = 1
    const val MAX_INTERVAL_HOURS: Int = 24

    fun validateName(name: String): String? =
        if (name.isBlank()) "Webhook name is required" else null

    fun validateUrl(url: String): String? {
        // Trim before checking so a leading space doesn't fail validation, and compare scheme
        // case-insensitively per RFC 3986 §3.1 — the repository trims before persisting anyway.
        val trimmed = url.trim()
        return when {
            trimmed.isBlank() -> "Webhook URL is required"
            !trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true) ->
                "Must start with http:// or https://"
            else -> null
        }
    }

    fun validateIntervalHours(text: String): String? {
        if (text.isBlank()) return "Enter a number between $MIN_INTERVAL_HOURS and $MAX_INTERVAL_HOURS"
        val value = text.toIntOrNull() ?: return "Enter a number between $MIN_INTERVAL_HOURS and $MAX_INTERVAL_HOURS"
        if (value !in MIN_INTERVAL_HOURS..MAX_INTERVAL_HOURS) {
            return "Must be between $MIN_INTERVAL_HOURS and $MAX_INTERVAL_HOURS hours"
        }
        return null
    }
}
