package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class EnparaBankParser : BankParser() {

    override fun getBankName() = "Enpara"

    override fun getCurrency() = "TRY"

    override fun canHandle(sender: String): Boolean {
        return sender.equals("Enpara", ignoreCase = true)
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()

        if (lower.contains("otp") ||
            lower.contains("doğrulama kodu") ||
            lower.contains("tek kullanımlık şifre") ||
            lower.contains("şifreniz")
        ) {
            return false
        }

        return lower.contains("harcama yapıldı") ||
                lower.contains("para transferi") ||
                lower.contains("giriş oldu")
    }

    override fun extractAmount(message: String): BigDecimal? {
        CARD_AMOUNT_REGEX.find(message)?.let {
            return parseTurkishNumber(it.groupValues[1])
        }

        OUTGOING_AMOUNT_REGEX.find(message)?.let {
            return parseTurkishNumber(it.groupValues[1])
        }

        INCOMING_AMOUNT_REGEX.find(message)?.let {
            return parseTurkishNumber(it.groupValues[1])
        }

        return null
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("giriş oldu") -> TransactionType.INCOME
            lower.contains("harcama yapıldı") -> TransactionType.EXPENSE
            lower.contains("para transferi") && lower.contains("yapıldı") -> TransactionType.EXPENSE
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        CARD_MERCHANT_REGEX.find(message)?.let { match ->
            val raw = match.groupValues[1].trim()
            val stripped = stripTrailingCountryCode(raw)
            val cleaned = cleanMerchantName(stripped)
            if (isValidMerchantName(cleaned)) return cleaned
        }

        OUTGOING_RECIPIENT_REGEX.find(message)?.let { match ->
            val cleaned = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(cleaned)) return cleaned
        }

        INCOMING_SENDER_REGEX.find(message)?.let { match ->
            val cleaned = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(cleaned)) return cleaned
        }

        return null
    }

    override fun extractAccountLast4(message: String): String? {
        CARD_LAST4_REGEX.find(message)?.let { match ->
            return extractLast4Digits(match.groupValues[1])
        }
        return null
    }

    override fun extractBalance(message: String): BigDecimal? {
        BALANCE_REGEX.find(message)?.let { match ->
            return parseTurkishNumber(match.groupValues[1])
        }
        return null
    }

    override fun detectIsCard(message: String): Boolean {
        return message.contains("Encard", ignoreCase = true)
    }

    private fun stripTrailingCountryCode(raw: String): String {
        return raw.trimEnd().removeSuffix(" TR").trimEnd()
    }

    private fun parseTurkishNumber(raw: String): BigDecimal? {
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
        private val CARD_AMOUNT_REGEX = Regex(
            """([0-9.,]+)\s*TL\s+tutarında\s+harcama\s+yapıldı""",
            RegexOption.IGNORE_CASE
        )

        private val OUTGOING_AMOUNT_REGEX = Regex(
            """([0-9.,]+)\s*TL\s+tutarında\s+para\s+transferi""",
            RegexOption.IGNORE_CASE
        )

        private val INCOMING_AMOUNT_REGEX = Regex(
            """sonucunda\s+([0-9.,]+)\s*TL\s+giriş\s+oldu""",
            RegexOption.IGNORE_CASE
        )

        private val CARD_MERCHANT_REGEX = Regex(
            """tarihinde\s+\d+\s*-\s*(.+?)\s+firmasında""",
            RegexOption.IGNORE_CASE
        )

        private val OUTGOING_RECIPIENT_REGEX = Regex(
            """hesabınızdan\s+(.+?)\s+adlı\s+alıcıya""",
            RegexOption.IGNORE_CASE
        )

        private val INCOMING_SENDER_REGEX = Regex(
            """tarihinde\s+(.+?)\s+tarafından""",
            RegexOption.IGNORE_CASE
        )

        private val CARD_LAST4_REGEX = Regex(
            """bağlı\s+(\d{4})\s+ile\s+biten\s+Encard""",
            RegexOption.IGNORE_CASE
        )

        private val BALANCE_REGEX = Regex(
            """İşlem\s+sonrası\s+hesap\s+bakiyesi:?\s*([0-9.,]+)\s*TL""",
            RegexOption.IGNORE_CASE
        )
    }
}
