package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for Liv Bank (UAE) - Digital bank
 * Inherits from UAEBankParser for multi-currency support.
 */
class LivBankParser : UAEBankParser() {

    override fun getBankName() = "Liv Bank"

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase().replace(Regex("\\s+"), "")
        return normalizedSender == "LIV" ||
                normalizedSender.contains("LIV") ||
                normalizedSender.matches(Regex("^[A-Z]{2}-LIV-[A-Z]$"))
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        if (lowerMessage.contains("otp") ||
            lowerMessage.contains("one time password") ||
            lowerMessage.contains("verification code") ||
            lowerMessage.contains("do not share") ||
            lowerMessage.contains("activation") ||
            lowerMessage.contains("has been blocked") ||
            lowerMessage.contains("has been activated") ||
            lowerMessage.contains("failed") ||
            lowerMessage.contains("declined") ||
            lowerMessage.contains("insufficient balance")
        ) {
            return false
        }

        val livTransactionKeywords = listOf(
            "has been credited",
            "purchase of",
            "debit card ending",
            "credit card ending"
        )

        if (livTransactionKeywords.any { lowerMessage.contains(it) }) {
            return true
        }

        return super.isTransactionMessage(message)
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val lowerMessage = message.lowercase()

        if (lowerMessage.contains("purchase of")) {
            val merchantPattern = Regex("""at\s+([^,]+?)(?:,|\s+Avl|\.\s)""", RegexOption.IGNORE_CASE)
            merchantPattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty() && !merchant.contains("Avl Balance")) {
                    return cleanMerchantName(merchant)
                }
            }

            val fallbackPattern = Regex("""at\s+([^.]+?)(?:\s+Avl|,)""", RegexOption.IGNORE_CASE)
            fallbackPattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty()) {
                    return cleanMerchantName(merchant)
                }
            }
        }

        if (lowerMessage.contains("has been credited")) {
            return "Account Credit"
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractAccountLast4(message: String): String? {
        super.extractAccountLast4(message)?.let { return it }
        val cardPattern = Regex("""(?:Debit|Credit)\s+Card ending\s+(\d{4})""", RegexOption.IGNORE_CASE)
        cardPattern.find(message)?.let {
            return it.groupValues[1]
        }

        val accountPattern = Regex("""account\s+([0-9A-Z]+)""", RegexOption.IGNORE_CASE)
        accountPattern.find(message)?.let { match ->
            return extractLast4Digits(match.groupValues[1])
        }

        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        val balancePatterns = listOf(
            Regex("""Current balance is\s+([A-Z]{3})\s+([\d,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""Avl Balance is\s+([A-Z]{3})\s+([\d,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""Balance:?\s+([A-Z]{3})\s+([\d,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in balancePatterns) {
            pattern.find(message)?.let { match ->
                val balanceStr = match.groupValues[2].replace(",", "")
                return try {
                    BigDecimal(balanceStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }

        // Fallback to FAB's multi-currency balance extraction
        return super.extractBalance(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        return when {
            lowerMessage.contains("has been credited") -> TransactionType.INCOME
            lowerMessage.contains("credited to account") -> TransactionType.INCOME
            lowerMessage.contains("refund") -> TransactionType.INCOME
            lowerMessage.contains("cashback") -> TransactionType.INCOME
            lowerMessage.contains("purchase of") -> TransactionType.EXPENSE
            lowerMessage.contains("debited") -> TransactionType.EXPENSE
            lowerMessage.contains("withdrawn") -> TransactionType.EXPENSE
            else -> super.extractTransactionType(message)
        }
    }

    override fun detectIsCard(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return lowerMessage.contains("debit card ending") ||
                lowerMessage.contains("credit card ending") ||
                lowerMessage.contains("purchase of") ||
                super.detectIsCard(message)
    }

    override fun containsCardPurchase(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return (lowerMessage.contains("purchase of") &&
                (lowerMessage.contains("debit card ending") || lowerMessage.contains("credit card ending"))) ||
                super.containsCardPurchase(message)
    }

    override fun extractCurrency(message: String): String? {
        val currencyPatterns = listOf(
            Regex("""purchase of\s+([A-Z]{3})\s+[\d,]+(?:\.\d{2})?""", RegexOption.IGNORE_CASE),
            Regex("""([A-Z]{3})\s+[\d,]+(?:\.\d{2})?[\s\n]+has been credited""", RegexOption.IGNORE_CASE),
            Regex("""([A-Z]{3})\s+[\d,]+(?:\.\d{2})?""", RegexOption.IGNORE_CASE)
        )

        for (pattern in currencyPatterns) {
            pattern.find(message)?.let { match ->
                val currencyCode = match.groupValues[1].uppercase()
                if (currencyCode.matches(Regex("""[A-Z]{3}""")) &&
                    !currencyCode.matches(Regex("""^(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)$""", RegexOption.IGNORE_CASE))
                ) {
                    return currencyCode
                }
            }
        }

        return "AED"
    }
}
