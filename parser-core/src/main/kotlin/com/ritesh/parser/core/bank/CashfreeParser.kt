package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class CashfreeParser : BankParser() {

    override fun getBankName() = "Cashfree"

    override fun getCurrency() = "INR"

    override fun canHandle(sender: String): Boolean {
        return sender.uppercase().contains("CSHFRE")
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        if (lowerMessage.contains("otp") ||
            lowerMessage.contains("one time password") ||
            lowerMessage.contains("verification code")
        ) {
            return false
        }

        if (lowerMessage.contains("payment") &&
            lowerMessage.contains("confirmed for order")
        ) {
            return true
        }

        return super.isTransactionMessage(message)
    }

    override fun extractAmount(message: String): BigDecimal? {
        val paymentPattern = Regex(
            """Payment\s+INR\s+([0-9,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        paymentPattern.find(message)?.let { match ->
            return try {
                BigDecimal(match.groupValues[1].replace(",", ""))
            } catch (e: NumberFormatException) {
                null
            }
        }

        return super.extractAmount(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        if (lowerMessage.contains("payment") &&
            lowerMessage.contains("confirmed for order")
        ) {
            return TransactionType.EXPENSE
        }

        return super.extractTransactionType(message)
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val merchantPattern = Regex(
            """confirmed\s+for\s+order\s+#\S+\s+on\s+([^.\n\r]+?)(?:\.|$)""",
            RegexOption.IGNORE_CASE
        )
        merchantPattern.find(message)?.let { match ->
            val cleaned = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(cleaned)) {
                return cleaned
            }
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractReference(message: String): String? {
        val idPattern = Regex(
            """\(ID:\s*([A-Za-z0-9]+)\)""",
            RegexOption.IGNORE_CASE
        )
        idPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return super.extractReference(message)
    }

    override fun extractAccountLast4(message: String): String? {
        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        return null
    }
}
