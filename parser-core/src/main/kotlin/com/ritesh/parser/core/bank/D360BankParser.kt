package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class D360BankParser : BankParser() {

    override fun getBankName() = "D360 Bank"

    override fun getCurrency() = "SAR"

    override fun canHandle(sender: String): Boolean {
        return sender.uppercase().contains("D360")
    }

    override fun extractAmount(message: String): BigDecimal? =
        FinancialMessageFields.sarSettlementOrAmount(message, listOf("Amount"))

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("incoming") -> TransactionType.INCOME
            lower.contains("refund") -> TransactionType.INCOME
            lower.contains("purchase") -> TransactionType.EXPENSE
            lower.contains("withdrawal") || lower.contains("withdraw") -> TransactionType.EXPENSE
            lower.contains("outgoing") -> TransactionType.EXPENSE
            lower.contains("payment") -> TransactionType.EXPENSE
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        Regex("""At\s*:\s*([^\n]+)""", RegexOption.IGNORE_CASE).findAll(message).forEach { match ->
            val candidate = match.groupValues[1].trim()
            if (DATE_LIKE.containsMatchIn(candidate)) return@forEach
            val merchant = cleanMerchantName(candidate)
            if (isValidMerchantName(merchant)) return merchant
        }

        Regex("""(?:Incoming|Outgoing) Transfer\s*:\s*([^\n]+)""", RegexOption.IGNORE_CASE)
            .find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) return merchant
            }

        return null
    }

    override fun extractAccountLast4(message: String): String? {
        Regex("""\*+(\d{4})\b""").find(message)?.let { return extractLast4Digits(it.groupValues[1]) }
        return super.extractAccountLast4(message)
    }

    override fun detectIsCard(message: String): Boolean {
        if (Regex("""Card\s*:""", RegexOption.IGNORE_CASE).containsMatchIn(message)) return true
        return super.detectIsCard(message)
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        if (SaudiTransactionMessageGuards.isDeclinedOrFailed(message) ||
            SaudiTransactionMessageGuards.isPromotionalOrOperationalNotice(message) ||
            FinancialMessageSafety.isSecurityCode(message)
        ) return false

        val promoExact = listOf("% off", "congratulations", "reward points", "unsubscribe")
        if (promoExact.any { lower.contains(it) } || PROMO_WORDS.containsMatchIn(lower)) return false

        val keywords = listOf(
            "purchase", "withdrawal", "transfer", "incoming", "outgoing", "account funding", "apple pay", "amount", "sar"
        )
        return keywords.any { lower.contains(it) }
    }

    private companion object {
        private val DATE_LIKE = Regex("""\d{4}-\d{2}-\d{2}""")

        private val PROMO_WORDS = Regex("""\b(?:offer|discount|sale|win|promo|click)\b""")
    }
}
