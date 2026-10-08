package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class ApolloParser : BankParser() {

    override fun getBankName() = "Apollo"

    override fun isMobileWallet() = true

    override fun getCurrency() = "ETB"  // Ethiopian Birr


    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase().trim()
        return upperSender == "APOLLO" ||
                upperSender.matches(Regex("""^[A-Z]{2}-APOLLO-[A-Z]$"""))
    }

    override fun extractAmount(message: String): BigDecimal? {
        val verbPattern = Regex(
            """(?:credited|debited)\s+with\s+ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        verbPattern.find(message)?.let { match ->
            return parseAmount(match.groupValues[1])
        }

        val firstEtbPattern = Regex("""ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        firstEtbPattern.find(message)?.let { match ->
            return parseAmount(match.groupValues[1])
        }

        return super.extractAmount(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        return when {
            lowerMessage.contains("credited with") -> TransactionType.INCOME
            lowerMessage.contains("debited with") -> TransactionType.EXPENSE
            else -> super.extractTransactionType(message)
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val byPattern = Regex(
            """credited\s+with\s+ETB\s*[0-9,]+(?:\.[0-9]{1,2})?\s+by\s+([^.]+?)\.""",
            RegexOption.IGNORE_CASE
        )
        byPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].replace(Regex("""\s+"""), " ").trim()
            if (merchant.isNotEmpty() && isValidMerchantName(merchant)) {
                return cleanMerchantName(merchant)
            }
        }

        return null
    }

    override fun extractAccountLast4(message: String): String? = null

    override fun extractBalance(message: String): BigDecimal? {
        val balancePattern = Regex(
            """Available\s+Balance:\s*ETB\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        balancePattern.find(message)?.let { match ->
            return parseAmount(match.groupValues[1])
        }

        return super.extractBalance(message)
    }

    private fun parseAmount(raw: String): BigDecimal? {
        return try {
            BigDecimal(raw.replace(",", ""))
        } catch (e: NumberFormatException) {
            null
        }
    }
}
