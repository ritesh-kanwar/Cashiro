package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for Nabil Bank (Nepal) SMS messages
 */
class NabilBankParser : BankParser() {

    override fun getBankName() = "Nabil Bank"

    override fun getCurrency() = "NPR"

    override fun canHandle(sender: String): Boolean {
        val s = sender.uppercase()
        return s.contains("NABIL") || s == "NABIL_ALERT" || s == "NABILBANK"
    }

    override fun extractAmount(message: String): BigDecimal? {
        // Reuse common NPR pattern
        val nprPattern = Regex("""NPR\s+([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        nprPattern.find(message)?.let { m ->
            val amountStr = m.groupValues[1].replace(",", "")
            return amountStr.toBigDecimalOrNull()
        }

        return super.extractAmount(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        if (lower.contains("withdrawn")) return TransactionType.EXPENSE
        if (lower.contains("deposited") || lower.contains("credited")) return TransactionType.INCOME
        return null
    }

    override fun extractReference(message: String): String? {
        // Primary: Remarks: MTXN0000517374-130
        val remarks = Regex("""Remarks[:\s]*([A-Z0-9\-~]+)""", RegexOption.IGNORE_CASE)
        remarks.find(message)?.let { return it.groupValues[1] }

        // Fallback: any MTXN-like token
        val refPattern = Regex("""(MTXN[0-9A-Z\-]+)""", RegexOption.IGNORE_CASE)
        refPattern.find(message)?.let { return it.groupValues[1] }

        return null
    }

    override fun extractAccountLast4(message: String): String? {
        // Look for visible digit groups and return last 4
        val digitPattern = Regex("""(\d{4,})""")
        digitPattern.findAll(message).forEach { match ->
            val num = match.groupValues[1]
            if (num.length >= 4) return num.takeLast(4)
        }
        return super.extractAccountLast4(message)
    }

}
