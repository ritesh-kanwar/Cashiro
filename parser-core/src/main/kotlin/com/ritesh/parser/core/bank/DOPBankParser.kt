package com.ritesh.parser.core.bank

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import java.text.Normalizer

<<<<<<< ours
/**
 * Parser for Department of Post (DOP) SMS messages
 */
class DOPBankParser : BankParser() {
=======
class DOPBankParser : BaseIndianBankParser() {
>>>>>>> theirs

    override fun getBankName() = "Department of Post"

    override fun canHandle(sender: String): Boolean {
<<<<<<< ours
        val normalizedSender = sender.uppercase()
        // Broaden sender support for DOP
        return normalizedSender.contains("DOPBNK") ||
                normalizedSender.contains("DEPARTMENT OF POST") ||
                normalizedSender.contains("DOP-") ||
                normalizedSender.endsWith("-DOP") ||
                normalizedSender == "DOP"
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        // Normalize Unicode text for potential RCS messages or weird spacing
=======
        val upper = sender.uppercase()
        return upper.contains("DOPBNK") ||
                upper.contains("DEPARTMENT OF POST") ||
                upper.contains("DOP-") ||
                upper.endsWith("-DOP") ||
                upper == "DOP"
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
>>>>>>> theirs
        val normalizedBody = normalizeUnicodeText(smsBody)
        return super.parse(normalizedBody, sender, timestamp)
    }

    private fun normalizeUnicodeText(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFKD)
<<<<<<< ours
            .replace(Regex("[^\\p{ASCII}]"), " ") // Replace non-ASCII with space
            .replace(Regex("\\s+"), " ") // Collapse multiple spaces
=======
            .replace(Regex("[^\\p{ASCII}]"), " ")
            .replace(Regex("\\s+"), " ")
>>>>>>> theirs
            .trim()
    }

    override fun extractAmount(message: String): BigDecimal? {
<<<<<<< ours
        // Pattern: amount Rs. 5550.00 or amount 5550.00
=======
>>>>>>> theirs
        val amountPattern = Regex(
            """amount\s+(?:Rs\.?|INR)?\s*([\d,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        amountPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }

        return super.extractAmount(message)
    }

    override fun extractAccountLast4(message: String): String? {
<<<<<<< ours
        // Pattern: Account No. XXXXXXXX1234
        // More flexible with spaces and dots
        val accountPattern = Regex(
            """Acc(?:ount)?\s*(?:No\.?)?\s+(?:[X\*]+)?(\d{4})""",
=======
        val accountPattern = Regex(
            """Acc(?:ount)?\s*(?:No\.?)?\s+(?:[X*]+)?(\d{4})""",
>>>>>>> theirs
            RegexOption.IGNORE_CASE
        )
        accountPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return super.extractAccountLast4(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
<<<<<<< ours
        val lowerMessage = message.lowercase()

        return when {
            lowerMessage.contains("credit") -> TransactionType.INCOME
            lowerMessage.contains("debit") -> TransactionType.EXPENSE
=======
        val lower = message.lowercase()
        return when {
            lower.contains("credit") -> TransactionType.INCOME
            lower.contains("debit") -> TransactionType.EXPENSE
>>>>>>> theirs
            else -> super.extractTransactionType(message)
        }
    }

    override fun extractBalance(message: String): BigDecimal? {
<<<<<<< ours
        // Pattern: Balance: Rs.40000.00
=======
>>>>>>> theirs
        val balancePattern = Regex(
            """Bal(?:ance)?\s*(?::)?\s*(?:Rs\.?|INR)?\s*([\d,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        balancePattern.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                null
            }
        }

        return super.extractBalance(message)
    }

    override fun extractReference(message: String): String? {
<<<<<<< ours
        // Pattern: [S76543210]
=======
>>>>>>> theirs
        val refPattern = Regex(
            """\[([A-Z0-9]+)\]""",
            RegexOption.IGNORE_CASE
        )
        refPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return super.extractReference(message)
    }

    override fun isTransactionMessage(message: String): Boolean {
<<<<<<< ours
        val lowerMessage = message.lowercase()
        // Broaden keywords for DOP
        val hasKeyKeywords = lowerMessage.contains("account") || lowerMessage.contains("a/c") || lowerMessage.contains("dop")
        val hasType = lowerMessage.contains("credit") || lowerMessage.contains("debit")
        
        return hasKeyKeywords && hasType
=======
        val lower = message.lowercase()
        val hasKeyword = lower.contains("account") || lower.contains("a/c") || lower.contains("dop")
        val hasType = lower.contains("credit") || lower.contains("debit")
        return hasKeyword && hasType
>>>>>>> theirs
    }
}
