package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class NDBBankParser : BankParser() {

    override fun getBankName() = "National Development Bank"

    override fun getCurrency() = "LKR"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase().replace(Regex("[\\s-]"), "")
        return normalized == "NDBALERT" || normalized == "NDB"
    }

    override fun extractAmount(message: String): BigDecimal? {
        val pattern = Regex(
            """LKR\s+([0-9,]+\.\d{2})\s+(?:credited|debited)""",
            RegexOption.IGNORE_CASE
        )
        pattern.find(message)?.let { match ->
            return match.groupValues[1].replace(",", "").toBigDecimalOrNull()
        }
        return null
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("credited to") -> TransactionType.INCOME
            lower.contains("debited from") -> TransactionType.EXPENSE
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val posPattern = Regex("""\bat\s+(.+?)\.\s*Avl\s+Bal""", RegexOption.IGNORE_CASE)
        posPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotEmpty()) return merchant
        }

        val descPattern = Regex("""\bas\s+(.+?)\.\s*Avl\s+Bal""", RegexOption.IGNORE_CASE)
        descPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotEmpty()) return merchant
        }

        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        val pattern = Regex("""Avl\s+Bal\s+(?:LKR\s+)?([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        pattern.find(message)?.let { match ->
            return match.groupValues[1].replace(",", "").toBigDecimalOrNull()
        }
        return null
    }

    override fun extractReference(message: String): String? {
        return null
    }

    override fun extractAccountLast4(message: String): String? {
        val pattern = Regex("""AC\s+X+(\d{4})(?!\d)""", RegexOption.IGNORE_CASE)
        return pattern.find(message)?.groupValues?.get(1)
    }
}
