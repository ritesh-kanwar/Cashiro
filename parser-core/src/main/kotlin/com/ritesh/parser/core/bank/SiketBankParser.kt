package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

class SiketBankParser : BankParser() {

    override fun getBankName() = "Siket Bank"

    override fun getCurrency() = "ETB"  // Ethiopian Birr

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase().trim()
        return normalized == "SIKET BANK" ||
                normalized == "SIKETBANK" ||
                normalized == "SIKET" ||
                normalized.matches(Regex("""^[A-Z]{2}-SIKET-[A-Z]$"""))
    }

    override fun extractAmount(message: String): BigDecimal? {
        val patterns = listOf(
            Regex("""Credited\s+with\s+ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""Debited\s+with\s+ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""transferred\s+ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return parseScaledAmount(match.groupValues[1])
            }
        }

        return super.extractAmount(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        return when {
            lowerMessage.contains("credited with") -> TransactionType.INCOME
            lowerMessage.contains("debited with") -> TransactionType.EXPENSE
            lowerMessage.contains("you have transferred") -> TransactionType.EXPENSE
            lowerMessage.contains("transferred etb") -> TransactionType.EXPENSE
            else -> super.extractTransactionType(message)
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val telebirrPattern = Regex(
            """to\s+(telebirr account\s+[+\d]+)""",
            RegexOption.IGNORE_CASE
        )
        telebirrPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotEmpty()) return merchant
        }

        return null
    }

    override fun extractAccountLast4(message: String): String? {
        val patterns = listOf(
            Regex("""Account\s+([\d*]+)""", RegexOption.IGNORE_CASE),
            Regex("""your account\s+([\d*]+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                extractLast4Digits(match.groupValues[1])?.let { return it }
            }
        }

        return super.extractAccountLast4(message)
    }

    override fun extractBalance(message: String): BigDecimal? {
        val balancePattern = Regex(
            """Current\s+Balance\s+is\s+ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        balancePattern.find(message)?.let { match ->
            return parseScaledAmount(match.groupValues[1])
        }

        return super.extractBalance(message)
    }

    override fun extractReference(message: String): String? {
        val refPattern = Regex(
            """Reference\s+number\s+([A-Z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        refPattern.find(message)?.let { match ->
            val ref = match.groupValues[1]
            if (ref.isNotEmpty()) return ref
        }

        return super.extractReference(message)
    }

    private fun parseScaledAmount(rawAmount: String): BigDecimal? {
        val normalized = rawAmount.replace(",", "")
        return try {
            BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP)
        } catch (e: NumberFormatException) {
            null
        }
    }
}
