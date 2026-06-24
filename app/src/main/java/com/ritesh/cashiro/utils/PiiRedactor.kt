package com.ritesh.cashiro.utils

object PiiRedactor {
    private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val UPI_REGEX = Regex("[a-zA-Z0-9._-]+@[a-zA-Z]{2,}")
    private val DIGITS_REGEX = Regex("\\b\\d{3,18}\\b")
    private val REF_REGEX = Regex("\\b(?=[A-Za-z]*\\d)(?=\\d*[A-Za-z])[A-Za-z\\d]{8,24}\\b")

    /**
     * Redacts PII from raw string.
     */
    fun redact(input: String?): String {
        if (input == null) return ""
        return input
            .replace(EMAIL_REGEX, "[EMAIL]")
            .replace(UPI_REGEX, "[UPI]")
            .replace(REF_REGEX, "[REF]")
            .replace(DIGITS_REGEX, "[MASKED]")
    }

    /**
     * Redacts card/account suffix or sensitive number.
     */
    fun redactSuffix(suffix: String?): String {
        if (suffix == null) return "null"
        return if (suffix.length >= 4) {
            "****" + suffix.takeLast(2)
        } else {
            "****"
        }
    }
}
