package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class PasargadBankParser : BaseIranianBankParser() {

    private val pattern = Regex(
        """([0-9.]+)\s+([+-][0-9,]+)\s+\d{2}/\d{2}_\d{2}:\d{2}\s+مانده:\s*([0-9,]+)"""
    )

    override fun getBankName(): String = "Pasargad Bank"

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        val pasargadSenders = setOf(
            "B.PASARGAD",
            "PASARGAD",
            "WEPOD"
        )
        return upperSender in pasargadSenders
    }

    override fun isTransactionMessage(message: String): Boolean {
        if (super.isTransactionMessage(message)) return true

        if (pattern.containsMatchIn(message.trim())) {
            val lower = message.lowercase()
            val nonTransactional = lower.contains("otp") ||
                lower.contains("رمز یکبار مصرف") ||
                lower.contains("کد تایید") ||
                lower.contains("تبلیغ") ||
                lower.contains("پیشنهاد") ||
                lower.contains("تخفیف") ||
                lower.contains("cashback offer") ||
                (lower.contains("درخواست") && lower.contains("پرداخت"))
            return !nonTransactional
        }
        return false
    }

    override fun extractAmount(message: String): BigDecimal? {
        pattern.find(message.trim())?.let { match ->
            val signedAmount = match.groupValues[2]
            val amountStr = signedAmount.drop(1).replace(",", "")
            return try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        }
        return null
    }

    override fun extractTransactionType(message: String): TransactionType? {
        pattern.find(message.trim())?.let { match ->
            return when (match.groupValues[2].firstOrNull()) {
                '+' -> TransactionType.INCOME
                '-' -> TransactionType.EXPENSE
                else -> null
            }
        }
        return null
    }

    override fun extractAccountLast4(message: String): String? {
        pattern.find(message.trim())?.let { match ->
            val pureNumeric = match.groupValues[1].replace(".", "")
            return if (pureNumeric.length >= 4) pureNumeric.takeLast(4) else null
        }
        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        pattern.find(message.trim())?.let { match ->
            val balanceStr = match.groupValues[3].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                null
            }
        }
        return null
    }
}
