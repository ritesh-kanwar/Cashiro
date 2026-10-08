package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class StandardCharteredNepalParser : BankParser() {

    override fun getBankName() = "Standard Chartered Bank Nepal"

    override fun getCurrency() = "NPR"

    override fun canHandle(sender: String): Boolean {
        val s = sender.uppercase()
        return s == "SC_ALERT"
    }

    override fun extractAmount(message: String): BigDecimal? {
        val nprPattern = Regex("""NPR\s+([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        nprPattern.find(message)?.let { m ->
            return m.groupValues[1].replace(",", "").toBigDecimalOrNull()
        }
        return super.extractAmount(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        if (lower.contains("debited from")) return TransactionType.EXPENSE
        if (lower.contains("deposited into")) return TransactionType.INCOME
        return null
    }

    override fun extractAccountLast4(message: String): String? {
        super.extractAccountLast4(message)?.let { return it }
        val accountPattern = Regex("""your account\s+(\d{4,})""", RegexOption.IGNORE_CASE)
        accountPattern.find(message)?.let { match ->
            return extractLast4Digits(match.groupValues[1])
        }
        return null
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val lower = message.lowercase()
        if (lower.contains("atm")) return "ATM Withdrawal"
        if (lower.contains("visa")) return "VISA Transaction"
        return super.extractMerchant(message, sender)
    }
}
