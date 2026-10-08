package com.ritesh.parser.core.bank

import java.math.BigDecimal

class PluxeeBankParser : BaseIndianBankParser() {

    override fun getBankName() = "Pluxee"

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("PLUXEE")
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val atBeforeBalance = Regex(
            """\bat\s+(.+?)\s*\.\s*Avl\s+bal""",
            RegexOption.IGNORE_CASE
        )
        atBeforeBalance.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractAccountLast4(message: String): String? {
        val cardPattern = Regex(
            """card\s+no\.?\s*(?:xx|XX|\*)*(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        cardPattern.find(message)?.let { match ->
            extractLast4Digits(match.groupValues[1])?.let { return it }
        }

        return super.extractAccountLast4(message)
    }
}
