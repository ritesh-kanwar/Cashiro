package com.ritesh.cashiro.data.webhook

object WebhookValidation {

    const val MIN_INTERVAL_HOURS: Int = 1
    const val MAX_INTERVAL_HOURS: Int = 24

    fun validateName(name: String): String? =
        if (name.isBlank()) "Webhook name is required" else null

    fun validateUrl(url: String): String? = when {
        url.isBlank() -> "Webhook URL is required"
        !url.startsWith("http://") && !url.startsWith("https://") ->
            "Must start with http:// or https://"
        else -> null
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
