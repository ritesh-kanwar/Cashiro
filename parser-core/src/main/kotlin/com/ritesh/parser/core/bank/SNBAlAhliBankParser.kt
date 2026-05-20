package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class SNBAlAhliBankParser : BankParser() {

    override fun getBankName() = "Saudi National Bank"

    override fun getCurrency() = "SAR"

    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("SNB") ||
                normalized.contains("ALAHLI") ||
                normalized.contains("AL-AHLI") ||
                normalized.contains("AL AHLI") ||
                sender.contains("الأهلي")
    }

    override fun extractAmount(message: String): BigDecimal? {
        val bPattern = Regex(
            """بـ\s*SAR\s*([0-9,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        bPattern.find(message)?.let { return parseSarAmount(it.groupValues[1]) }

        val amountPattern = Regex(
            """مبلغ\s*:?\s*SAR\s*([0-9,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        amountPattern.find(message)?.let { return parseSarAmount(it.groupValues[1]) }

        val looseSarPattern = Regex(
            """SAR\s+([0-9,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        looseSarPattern.find(message)?.let { return parseSarAmount(it.groupValues[1]) }

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
            message.contains("واردة") -> TransactionType.INCOME
            message.contains("إيداع") -> TransactionType.INCOME
            message.contains("شراء") -> TransactionType.EXPENSE
            message.contains("سحب") -> TransactionType.EXPENSE
            message.contains("صادرة") -> TransactionType.EXPENSE
            message.contains("خصم") -> TransactionType.EXPENSE
            message.contains("سداد") -> TransactionType.EXPENSE
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val fromPattern = Regex("""من\s+([^\n]+?)(?:\n|$)""")
        fromPattern.find(message)?.let { match ->
            val raw = match.groupValues[1].trim()
            if (raw.isNotBlank() && !raw.all { it == '*' || it.isDigit() }) {
                val merchant = cleanMerchantName(raw)
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }

        val toPattern = Regex("""الى\s*:?\s*([^\n]+?)(?:\n|$)""")
        toPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        if (message.contains("صراف")) {
            return "ATM Withdrawal"
        }

        return null
    }

    override fun extractAccountLast4(message: String): String? {
        val madaPattern = Regex("""مدى\s*\*+\s*(\d{3,4})""")
        madaPattern.find(message)?.let { return extractLast4Digits(it.groupValues[1]) }

        val cardPattern = Regex("""بطاقة\s*\*+\s*(\d{3,4})""")
        cardPattern.find(message)?.let { return extractLast4Digits(it.groupValues[1]) }

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
        if (message.contains("مدى") || message.contains("بطاقة") ||
            message.contains("نقاط بيع") || message.contains("SamsungPay", ignoreCase = true) ||
            message.contains("ApplePay", ignoreCase = true)
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
            "خصم",
            "سداد",
            "إيداع",
            "SAR"
        )
        return keywords.any { message.contains(it) }
    }
}
