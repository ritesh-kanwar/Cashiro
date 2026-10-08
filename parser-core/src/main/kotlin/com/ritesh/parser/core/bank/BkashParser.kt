package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class BkashParser : BankParser() {

    override fun getBankName() = "bKash"

    override fun getCurrency() = "BDT"

    override fun canHandle(sender: String): Boolean {
        return sender.uppercase().contains("BKASH")
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("you have received") ||
                lower.contains("cash in") ||
                lower.contains("payment of") ||
                lower.contains("send money")
    }

    override fun extractAmount(message: String): BigDecimal? {
        val pattern = Regex("""Tk\s+([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        pattern.find(message)?.let { match ->
            return match.groupValues[1].replace(",", "").toBigDecimalOrNull()
        }
        return null
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("you have received") -> TransactionType.INCOME
            lower.contains("cash in") -> TransactionType.INCOME
            lower.contains("payment of") -> TransactionType.EXPENSE
            lower.contains("send money") -> TransactionType.EXPENSE
            else -> null
        }
    }

    override fun extractBalance(message: String): BigDecimal? {
        val pattern = Regex("""Balance\s+Tk\s+([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        pattern.find(message)?.let { match ->
            return match.groupValues[1].replace(",", "").toBigDecimalOrNull()
        }
        return null
    }

    override fun extractReference(message: String): String? {
        val pattern = Regex("""TrxID\s+([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)
        pattern.find(message)?.let { return it.groupValues[1] }
        return null
    }

    override fun extractMerchant(message: String, sender: String): String? = null

    override fun extractAccountLast4(message: String): String? = null
}
