package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class GTBankParser : BankParser() {

    override fun getBankName() = "GTBank"

    override fun getCurrency() = "NGN"

    override fun canHandle(sender: String): Boolean {
        val upper = sender.uppercase()
        return upper.contains("GTBANK") ||
                upper.contains("GTB") ||
                upper.contains("GUARANTY")
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        if (lower.contains("otp") || lower.contains("verification code")) {
            return false
        }
        return Regex("""(?i)Amt:\s*NGN\s*[0-9,]+(?:\.\d{1,2})?\s*(DR|CR)\b""")
            .containsMatchIn(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val match = Regex("""(?i)Amt:\s*NGN\s*[0-9,]+(?:\.\d{1,2})?\s*(DR|CR)\b""")
            .find(message) ?: return null
        return when (match.groupValues[1].uppercase()) {
            "DR" -> TransactionType.EXPENSE
            "CR" -> TransactionType.INCOME
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
        val match = Regex("""(?i)Acct:\s*\**([0-9]+)""").find(message) ?: return null
        return extractLast4Digits(match.groupValues[1])
    }
}
