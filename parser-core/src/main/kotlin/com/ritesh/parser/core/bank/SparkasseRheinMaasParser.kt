package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class SparkasseRheinMaasParser : BankParser() {

    override fun getBankName() = "Sparkasse Rhein-Maas"

    override fun getCurrency() = "EUR"

    override fun canHandle(sender: String): Boolean {
        return sender.uppercase().contains("SPARKASSE")
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()

        if (lower.contains("otp") ||
            lower.contains("tan") && lower.contains("code") ||
            lower.contains("verifizierungscode")
        ) {
            return false
        }

        if (lower.contains("kontostandswecker")) {
            return false
        }

        return lower.contains("kartenwecker") || lower.contains("gehaltswecker")
    }

    override fun extractAmount(message: String): BigDecimal? {
        val transactionLine = findTransactionLine(message) ?: return null
        val amountMatch = TRANSACTION_AMOUNT_REGEX.find(transactionLine) ?: return null
        val raw = amountMatch.groupValues[2]
        return parseGermanNumber(raw)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        if (lower.contains("gehaltswecker")) {
            return TransactionType.INCOME
        }

        val transactionLine = findTransactionLine(message)
        if (transactionLine != null) {
            val signMatch = TRANSACTION_AMOUNT_REGEX.find(transactionLine)
            val sign = signMatch?.groupValues?.get(1)
            if (sign == "+") return TransactionType.INCOME
            if (sign == "-") return TransactionType.EXPENSE
        }

        if (lower.contains("kartenwecker")) {
            return TransactionType.EXPENSE
        }
        return null
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val transactionLine = findTransactionLine(message) ?: return null
        val colonIdx = transactionLine.indexOf(':')
        if (colonIdx <= 0) return null
        val candidate = transactionLine.substring(0, colonIdx).trim()
        val cleaned = cleanMerchantName(candidate)
        return if (isValidMerchantName(cleaned)) cleaned else null
    }

    override fun extractAccountLast4(message: String): String? {
        ACCOUNT_REGEX.find(message)?.let { match ->
            return extractLast4Digits(match.groupValues[1])
        }
        return super.extractAccountLast4(message)
    }

    override fun extractBalance(message: String): BigDecimal? {
        BALANCE_REGEX.find(message)?.let { match ->
            return parseGermanNumber(match.groupValues[1])
        }
        return null
    }

    override fun detectIsCard(message: String): Boolean {
        return message.lowercase().contains("kartenwecker") ||
                message.lowercase().contains("kartenumsatz")
    }

    private fun findTransactionLine(message: String): String? {
        for (rawLine in message.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.lowercase().startsWith("neuer saldo")) continue
            if (TRANSACTION_AMOUNT_REGEX.containsMatchIn(line)) {
                return line
            }
        }
        return null
    }

    private fun parseGermanNumber(raw: String): BigDecimal? {
        val normalized = raw
            .replace(".", "")
            .replace(",", ".")
        return try {
            BigDecimal(normalized)
        } catch (e: NumberFormatException) {
            null
        }
    }

    companion object {
        private val TRANSACTION_AMOUNT_REGEX = Regex(
            """([+-])?\s*(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?|\d+(?:,\d{1,2})?)\s*EUR""",
            RegexOption.IGNORE_CASE
        )

        private val BALANCE_REGEX = Regex(
            """Neuer\s+Saldo:?\s*([+-]?\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?|\d+(?:,\d{1,2})?)\s*EUR""",
            RegexOption.IGNORE_CASE
        )

        private val ACCOUNT_REGEX = Regex(
            """Konto\s*\*+\s*(\d{3,})""",
            RegexOption.IGNORE_CASE
        )
    }
}
