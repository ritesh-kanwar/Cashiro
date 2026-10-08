package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class NationalSavingsBankParser : BankParser() {

    override fun getBankName() = "National Savings Bank"

    override fun getCurrency() = "LKR"

    override fun canHandle(sender: String): Boolean {
        val s = sender.uppercase()
        return s == "NSB" || s.endsWith("-NSB") || s.contains("-NSB-")
    }

    override fun extractAmount(message: String): BigDecimal? {
        val pattern = Regex(
            """LKR\s+([0-9,]+\.\d{2})\s+(?:Credited|Debited)""",
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
        val posPattern = Regex("""@\s+(.+?)\.\s""", RegexOption.IGNORE_CASE)
        posPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotEmpty()) return merchant
        }

        val descPattern = Regex(
            """AvlBal\s+LKR\s+[0-9,]+\.\d{2}\.\s*(.+?)\.\s*Thank you""",
            RegexOption.IGNORE_CASE
        )
        descPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim().removePrefix("Transaction ").trim()
            if (merchant.isNotEmpty()) return merchant
        }

        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        val pattern = Regex("""AvlBal\s+LKR\s+([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        pattern.find(message)?.let { match ->
            return match.groupValues[1].replace(",", "").toBigDecimalOrNull()
        }
        return null
    }

    override fun extractAccountLast4(message: String): String? {
        val pattern = Regex("""A/c\s+X+(\d{4})(?!\d)""", RegexOption.IGNORE_CASE)
        return pattern.find(message)?.groupValues?.get(1)
    }
}
