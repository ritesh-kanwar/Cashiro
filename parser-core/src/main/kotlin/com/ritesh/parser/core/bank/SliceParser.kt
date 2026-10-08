package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class SliceParser : BankParser() {

    override fun getBankName() = "Slice"

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("SLICE") ||
                normalizedSender.contains("SLICEIT") ||
                normalizedSender.contains("SLCEIT") ||
                normalizedSender.contains("SLCBNK")
    }

    private fun isSuccessMessage(message: String): Boolean {
        val lower = message.lowercase()

        return Regex("""\bsuccessful\b""").containsMatchIn(lower) ||
               Regex("""\bsuccess\b""").containsMatchIn(lower) ||
               lower.contains("approved") ||
               lower.contains("confirmed")
    }

    private fun isFailureMessage(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("declined") ||
               lower.contains("failed") ||
               lower.contains("rejected") ||
               lower.contains("error") ||
               lower.contains("denied") ||
               lower.contains("unsuccessful")
    }

    private fun isDatePhrase(text: String): Boolean {

        val datePattern = Regex("""\b(?:\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+\d{1,2})\b""", RegexOption.IGNORE_CASE)
        return datePattern.containsMatchIn(text)
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        if (lowerMessage.contains("otp")) {
            return false
        }

        if (lowerMessage.contains("revoked") ||
            lowerMessage.contains("is paused") ||
            lowerMessage.contains("is suspended")) {
            return false
        }

        if (lowerMessage.contains("sent")) {
            return true
        }

        if (lowerMessage.contains("transaction")) {
            return isSuccessMessage(message) && !isFailureMessage(message)
        }

        return super.isTransactionMessage(message)
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val lowerMessage = message.lowercase()

        val atMerchantPattern = Regex(
            """\bat\s+(.+?)(?:\s+from\b|\s+on\b|\s+is\b|\.\s|$)""",
            RegexOption.IGNORE_CASE
        )
        atMerchantPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        if (Regex("""\ba/c\b""", RegexOption.IGNORE_CASE).containsMatchIn(message)) {
            val payeeKeyword = if (lowerMessage.contains("received")) "from" else "to"
            val payeePattern = Regex(

                """\b$payeeKeyword\s+(.+?)(?:\s+in\s+your\b|\s+on\b|\s+via\b|\s+is\b|\s*\(|\.\s|$)""",
                RegexOption.IGNORE_CASE
            )
            payeePattern.find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }

        val sentToPattern = Regex("""sent.*to\s+([A-Z][A-Z0-9\s./&-]+?)\s*\(""", RegexOption.IGNORE_CASE)
        sentToPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotEmpty()) {
                return cleanMerchantName(merchant)
            }
        }

        val fromPattern =
            Regex("""from\s+([A-Z][A-Z0-9\s]+?)(?:\s+on|\s+\(|$)""", RegexOption.IGNORE_CASE)
        fromPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotEmpty() && !merchant.equals("NEFT", ignoreCase = true)) {
                return cleanMerchantName(merchant)
            }
        }

        val onPattern = Regex("""\bon\s+([A-Za-z0-9\s./&-]+?)(?:\s+is|$)""", RegexOption.IGNORE_CASE)
        onPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotEmpty() &&
                !merchant.equals("slice", ignoreCase = true) &&
                !merchant.equals("RS", ignoreCase = true) &&
                !isDatePhrase(merchant)) {
                return cleanMerchantName(merchant)
            }
        }

        return when {
            lowerMessage.contains("paypal") -> "PayPal"
            lowerMessage.contains("slice") && lowerMessage.contains("credited") -> "Slice Credit"
            else -> super.extractMerchant(message, sender) ?: "Slice"
        }
    }

    override fun detectIsCard(message: String): Boolean {
        val lower = message.lowercase()

        if (lower.contains("transaction of") &&
            Regex("""\bat\s""", RegexOption.IGNORE_CASE).containsMatchIn(message) &&
            !lower.contains("sent") &&
            !lower.contains("received") &&
            !lower.contains("paid")) {
            return true
        }
        return super.detectIsCard(message)
    }

    override fun extractBalance(message: String): BigDecimal? {

        val sliceBal = Regex(
            """Avl\.?\s*Bal\.?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        sliceBal.find(message)?.let { match ->
            return try {
                BigDecimal(match.groupValues[1].replace(",", ""))
            } catch (e: NumberFormatException) {
                null
            }
        }

        return super.extractBalance(message)
    }

    override fun extractReference(message: String): String? {

        val upiRef = Regex("""UPI\s+Ref(?:\s+ID)?[:\s]+([0-9]+)""", RegexOption.IGNORE_CASE)
        upiRef.find(message)?.let { return it.groupValues[1] }

        val refId = Regex("""Ref\s+ID[:\s]+([0-9]+)""", RegexOption.IGNORE_CASE)
        refId.find(message)?.let { return it.groupValues[1] }

        return super.extractReference(message)
    }

    private fun hasCardContext(lowerMessage: String): Boolean {
        return lowerMessage.contains("credit card") ||
                lowerMessage.contains("credit limit") ||
                lowerMessage.contains("available limit") ||
                lowerMessage.contains("card ending") ||
                lowerMessage.contains("card xx") ||
                lowerMessage.contains("card no") ||
                lowerMessage.contains("on your slice card") ||
                lowerMessage.contains("slice card")
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        val cardContext = hasCardContext(lowerMessage)

        return when {

            lowerMessage.contains("credited") -> TransactionType.INCOME
            lowerMessage.contains("received") -> TransactionType.INCOME
            lowerMessage.contains("cashback") -> TransactionType.INCOME
            lowerMessage.contains("refund") -> TransactionType.INCOME

            lowerMessage.contains("debited") -> TransactionType.EXPENSE
            lowerMessage.contains("sent") -> TransactionType.EXPENSE
            lowerMessage.contains("spent") ->
                if (cardContext) TransactionType.CREDIT else TransactionType.EXPENSE
            lowerMessage.contains("paid") ->
                if (cardContext) TransactionType.CREDIT else TransactionType.EXPENSE
            lowerMessage.contains("payment") && !lowerMessage.contains("received") ->
                if (cardContext) TransactionType.CREDIT else TransactionType.EXPENSE

            lowerMessage.contains("transaction") && !lowerMessage.contains("credited") &&
                isSuccessMessage(message) && !isFailureMessage(message) ->
                if (cardContext) TransactionType.CREDIT else TransactionType.EXPENSE

            else -> super.extractTransactionType(message)
        }
    }
}
