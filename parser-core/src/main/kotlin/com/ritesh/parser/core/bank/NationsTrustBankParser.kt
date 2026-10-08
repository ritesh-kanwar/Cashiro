package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class NationsTrustBankParser : BankParser() {

    override fun getBankName() = "Nations Trust Bank"

    override fun getCurrency() = "LKR"

    override fun canHandle(sender: String): Boolean {
        return sender.uppercase().contains("NATIONSSMS")
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        if (lower.contains("made to card")) {
            return false
        }
        return lower.contains("approved on your card")
    }

    override fun extractTransactionType(message: String): TransactionType? {
        return if (message.contains("approved on your card", ignoreCase = true)) {
            TransactionType.CREDIT
        } else {
            null
        }
    }

    override fun extractAmount(message: String): BigDecimal? {
        val pattern = Regex("""for\s+LKR\s+([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        pattern.find(message)?.let { match ->
            return match.groupValues[1].replace(",", "").toBigDecimalOrNull()
        }
        return null
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val pattern = Regex("""\bat\s+(.+?)\s+Available\s+Bal""", RegexOption.IGNORE_CASE)
        pattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotEmpty()) return merchant
        }
        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        val pattern = Regex("""Available\s+Bal\s+LKR\s+([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        pattern.find(message)?.let { match ->
            return match.groupValues[1].replace(",", "").toBigDecimalOrNull()
        }
        return null
    }

    override fun extractAccountLast4(message: String): String? {
        val pattern = Regex("""Card\s+\d+\*+(\d{4})(?!\d)""", RegexOption.IGNORE_CASE)
        return pattern.find(message)?.groupValues?.get(1)
    }
}
