package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class MoniepointParser : BankParser() {

    override fun getBankName() = "Moniepoint"

    override fun getCurrency() = "NGN"

    override fun canHandle(sender: String): Boolean {
        val upper = sender.uppercase()
        return upper.contains("MONIEPOINT") || upper.contains("MONNIFY")
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        if (lower.contains("otp") || lower.contains("verification code")) {
            return false
        }
        return Regex("""(?i)\b(credit|debit)\s+alert\b""").containsMatchIn(message) &&
                Regex("""(?i)Amt:\s*NGN""").containsMatchIn(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        return when {
            Regex("""(?i)\bcredit\s+alert\b""").containsMatchIn(message) -> TransactionType.INCOME
            Regex("""(?i)\bdebit\s+alert\b""").containsMatchIn(message) -> TransactionType.EXPENSE
            else -> null
        }
    }

    override fun extractAmount(message: String): BigDecimal? {
        val match = Regex("""(?i)Amt:\s*NGN\s*([0-9,]+(?:\.\d{1,2})?)""").find(message) ?: return null
        return try {
            BigDecimal(match.groupValues[1].replace(",", ""))
        } catch (e: NumberFormatException) {
            null
        }
    }

    override fun extractBalance(message: String): BigDecimal? {
        val match = Regex("""(?i)\bBal:\s*NGN\s*([0-9,]+(?:\.\d{1,2})?)""").find(message) ?: return null
        return try {
            BigDecimal(match.groupValues[1].replace(",", ""))
        } catch (e: NumberFormatException) {
            null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val match = Regex("""(?i)Desc:[ \t]*(.+)""").find(message) ?: return null
        val desc = match.groupValues[1].trim()
        return desc.ifBlank { null }
    }

    override fun extractAccountLast4(message: String): String? {
        val match = Regex("""(?i)Acc:\s*([0-9*]+)""").find(message) ?: return null
        return extractLast4Digits(match.groupValues[1])
    }
}
