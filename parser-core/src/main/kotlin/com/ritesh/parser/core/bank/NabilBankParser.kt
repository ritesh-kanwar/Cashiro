package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

<<<<<<< ours
/**
 * Parser for Nabil Bank (Nepal) SMS messages
 */
class NabilBankParser : BankParser() {

    companion object {
        private val accountKeywordPatterns = listOf(
            Regex(
                """(?i)\b(?:acct|a/c|account|card|ending|last)\b(?:\s+(?:no\.?|number))?[\s:.#\-]*([0-9]{4,})\b"""
            ),
            Regex(
                """(?i)\b(?:no\.?|number)\b[\s:.#\-]*([0-9]{4,})\b"""
            )
        )

        private val fallbackPattern = Regex(
            """(?<![A-Za-z0-9/₹$€£])([0-9]{4,})(?![A-Za-z0-9/₹$€£])"""
        )
    }

=======
class NabilBankParser : BankParser() {

>>>>>>> theirs
    override fun getBankName() = "Nabil Bank"

    override fun getCurrency() = "NPR"

    override fun canHandle(sender: String): Boolean {
        val s = sender.uppercase()
        return s.contains("NABIL") || s == "NABIL_ALERT" || s == "NABILBANK"
    }

    override fun extractAmount(message: String): BigDecimal? {
<<<<<<< ours
        // Reuse common NPR pattern
=======
>>>>>>> theirs
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
<<<<<<< ours
        // Primary: Remarks: MTXN0000517374-130
        val remarks = Regex("""Remarks[:\s]*([A-Z0-9\-~]+)""", RegexOption.IGNORE_CASE)
        remarks.find(message)?.let { return it.groupValues[1] }

        // Fallback: any MTXN-like token
=======
        val remarks = Regex("""Remarks[:\s]*([A-Z0-9\-~]+)""", RegexOption.IGNORE_CASE)
        remarks.find(message)?.let { return it.groupValues[1] }

>>>>>>> theirs
        val refPattern = Regex("""(MTXN[0-9A-Z\-]+)""", RegexOption.IGNORE_CASE)
        refPattern.find(message)?.let { return it.groupValues[1] }

        return null
    }

    override fun extractAccountLast4(message: String): String? {
<<<<<<< ours
        val normalizedMessage = message.replace('\u00A0', ' ')

        accountKeywordPatterns.forEach { pattern ->
            pattern.find(normalizedMessage)?.let { match ->
                val last4 = match.groupValues[1].takeLast(4)
                if (super.isValidAccountLast4(last4, match.value, normalizedMessage)) {
                    return last4
                }
            }
        }

        fallbackPattern.findAll(normalizedMessage).forEach { match ->
            val last4 = match.groupValues[1].takeLast(4)
            if (super.isValidAccountLast4(last4, match.value, normalizedMessage)) {
                return last4
            }
        }

        return null
    }

=======
        val maskedPattern = Regex("""#+(\d{4,})""")
        maskedPattern.find(message)?.let { match ->
            return extractLast4Digits(match.groupValues[1])
        }
        return super.extractAccountLast4(message)
    }
>>>>>>> theirs
}
