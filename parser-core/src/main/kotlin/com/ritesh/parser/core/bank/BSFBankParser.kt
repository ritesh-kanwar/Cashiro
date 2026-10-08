package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class BSFBankParser : BankParser() {

    override fun getBankName() = "Banque Saudi Fransi"

    override fun getCurrency() = "SAR"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase().replace(Regex("[\\s\\-_]"), "")
        if (normalized == "BSF" || normalized.contains("BSFR")) return true
        if (Regex("""(?:^|[^A-Z])BSF(?:[^A-Z]|$)""").containsMatchIn(sender.uppercase())) return true
        if (sender.contains("الفرنسي")) return true
        return false
    }

    override fun extractAmount(message: String): BigDecimal? {
        val amountLabel = """(?:القيمة|مبلغ)"""

        Regex(
            """$amountLabel\s*:?\s*SAR\s*([0-9,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        ).find(message)?.let { return parseSarAmount(it.groupValues[1]) }

        Regex(
            """$amountLabel\s*:?\s*([0-9,]+(?:\.\d{1,2})?)\s*SAR""",
            RegexOption.IGNORE_CASE
        ).find(message)?.let { return parseSarAmount(it.groupValues[1]) }

        return null
    }

    private fun parseSarAmount(raw: String): BigDecimal? {
        val cleaned = raw.replace(",", "")
        return try {
            BigDecimal(cleaned)
        } catch (e: NumberFormatException) {
            null
        }
    }

    override fun extractTransactionType(message: String): TransactionType? {
        return when {
            message.contains("واردة") -> TransactionType.INCOME   // incoming transfer
            message.contains("صادرة") -> TransactionType.EXPENSE  // outgoing transfer
            message.contains("خصم") -> TransactionType.EXPENSE    // debited
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val isIncoming = message.contains("واردة")
        val isOutgoing = message.contains("صادرة")

        if (isOutgoing) {
            Regex("""إلى\s*:?\s*([^\n]+?)(?:\n|$)""").find(message)?.let { match ->
                cleanCounterparty(match.groupValues[1])?.let { return it }
            }
        }

        if (isIncoming) {
            Regex("""من\s*:?\s*([^\n]+?)(?:\n|$)""").find(message)?.let { match ->
                cleanCounterparty(match.groupValues[1])?.let { return it }
            }
        }

        return null
    }

    private fun cleanCounterparty(raw: String): String? {
        var value = raw.trim().trimEnd('*', '×', ' ', '\t')
        if (value.isBlank()) return null
        if (value.all { it == '*' || it == '×' || it.isDigit() || it.isWhitespace() }) return null
        val cleaned = cleanMerchantName(value)
        return if (isValidMerchantName(cleaned)) cleaned else null
    }

    override fun extractAccountLast4(message: String): String? {
        Regex("""حساب\s*\*+\s*(\d{3,4})""").find(message)?.let {
            return extractLast4Digits(it.groupValues[1])
        }
        return null
    }

    override fun extractReference(message: String): String? {
        Regex("""آيبان\s*:?\s*([\*A-Z0-9]+)""", RegexOption.IGNORE_CASE)
            .find(message)?.let {
                val value = it.groupValues[1].trim()
                if (value.isNotBlank()) return value
            }
        return null
    }

    override fun isTransactionMessage(message: String): Boolean {
        if (message.contains("رمز") || message.contains("OTP", ignoreCase = true) ||
            message.contains("كلمة المرور")
        ) {
            return false
        }
        val keywords = listOf(
            "حوالة",   // transfer
            "واردة",   // incoming
            "صادرة",   // outgoing
            "خصم",     // debit
            "SAR"
        )
        return keywords.any { message.contains(it) }
    }
}
