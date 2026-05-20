package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class SabbBankParser : BankParser() {

    override fun getBankName() = "SABB"

    override fun getCurrency() = "SAR"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase().replace(Regex("[\\s\\-_]"), "")
        if (normalized == "SAB" || normalized == "SABB") return true
        if (normalized.contains("SABBANK") || normalized.contains("SABB")) return true
        if (Regex("""(?:^|[^A-Z])SAB(?:[^A-Z]|$)""").containsMatchIn(sender.uppercase())) return true
        if (sender.contains("ساب") || sender.contains("الأول")) return true
        return false
    }

    override fun extractAmount(message: String): BigDecimal? {
        val amountSarFirst = Regex(
            """مبلغ\s*:?\s*SAR\s*([0-9,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        amountSarFirst.find(message)?.let { return parseSarAmount(it.groupValues[1]) }

        val amountSarLast = Regex(
            """مبلغ\s*:?\s*([0-9,]+(?:\.\d{1,2})?)\s*SAR""",
            RegexOption.IGNORE_CASE
        )
        amountSarLast.find(message)?.let { return parseSarAmount(it.groupValues[1]) }

        val looseSar = Regex(
            """SAR\s+([0-9,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        looseSar.find(message)?.let { return parseSarAmount(it.groupValues[1]) }

        return null
    }

    private fun parseSarAmount(raw: String): BigDecimal? {
        val cleaned = raw.replace(",", "")
        return try {
            BigDecimal(cleaned)
        } catch (e: NumberFormatException) {
            null
        }
    }

    override fun extractTransactionType(message: String): TransactionType? {
        return when {
            message.contains("إيداع") -> TransactionType.INCOME
            message.contains("واردة") -> TransactionType.INCOME

            message.contains("صادرة") -> TransactionType.EXPENSE
            message.contains("شراء") -> TransactionType.EXPENSE
            message.contains("سحب") -> TransactionType.EXPENSE
            message.contains("خصم") -> TransactionType.EXPENSE
            message.contains("سداد") -> TransactionType.EXPENSE
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val isIncoming = message.contains("إيداع") || message.contains("واردة")
        val isOutgoingTransfer = message.contains("صادرة")

        val ladaPattern = Regex("""لدى\s*:?\s*([^\n]+?)(?:\n|في\s*:|$)""")
        ladaPattern.find(message)?.let { match ->
            cleanSabbMerchant(match.groupValues[1])?.let { return it }
        }

        if (isOutgoingTransfer) {
            val toPattern = Regex("""إلى\s*:?\s*([^\n]+?)(?:\n|في\s*:|$)""")
            toPattern.find(message)?.let { match ->
                cleanSabbMerchant(match.groupValues[1])?.let { return it }
            }
        }

        if (isIncoming) {
            val fromPattern = Regex("""من\s*:?\s*([^\n]+?)(?:\n|في\s*:|$)""")
            fromPattern.find(message)?.let { match ->
                cleanSabbMerchant(match.groupValues[1])?.let { return it }
            }
        }

        return null
    }

    private fun cleanSabbMerchant(raw: String): String? {
        var value = raw.trim()
        value = value.trimEnd('×', '*', ' ', '\t')
        if (value.isBlank()) return null
        if (value.all { it == '*' || it == '×' || it.isDigit() || it.isWhitespace() }) return null
        val cleaned = cleanMerchantName(value)
        return if (isValidMerchantName(cleaned)) cleaned else null
    }

    override fun extractAccountLast4(message: String): String? {
        val cardPattern = Regex("""بطاقة\s*:?\s*\*+\s*(\d{3,4})""")
        cardPattern.find(message)?.let { return extractLast4Digits(it.groupValues[1]) }

        val ownAccountPatterns = listOf(
            Regex("""من\s*:?\s*\*+\s*(\d{3,4})"""),
            Regex("""إلى\s*:?\s*\*+\s*(\d{3,4})""")
        )
        for (pattern in ownAccountPatterns) {
            pattern.find(message)?.let { return extractLast4Digits(it.groupValues[1]) }
        }

        return super.extractAccountLast4(message)
    }

    override fun extractBalance(message: String): BigDecimal? {
        val balancePattern = Regex(
            """الرصيد(?:\s*المتاح)?\s*:?\s*SAR\s*([0-9,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        balancePattern.find(message)?.let { return parseSarAmount(it.groupValues[1]) }
        return null
    }

    override fun detectIsCard(message: String): Boolean {
        if (message.contains("بطاقة") ||
            message.contains("مدى") ||
            message.contains("نقاط البيع") ||
            message.contains("SamsungPay", ignoreCase = true) ||
            message.contains("Samsung Pay", ignoreCase = true) ||
            message.contains("ApplePay", ignoreCase = true) ||
            message.contains("Apple Pay", ignoreCase = true)
        ) {
            return true
        }
        return super.detectIsCard(message)
    }

    override fun isTransactionMessage(message: String): Boolean {
        if (message.contains("رمز") || message.contains("OTP", ignoreCase = true) ||
            message.contains("كلمة المرور")
        ) {
            return false
        }

        val keywords = listOf(
            "شراء",
            "سحب",
            "حوالة",
            "إيداع",
            "خصم",
            "سداد",
            "SAR"
        )
        return keywords.any { message.contains(it) }
    }
}
