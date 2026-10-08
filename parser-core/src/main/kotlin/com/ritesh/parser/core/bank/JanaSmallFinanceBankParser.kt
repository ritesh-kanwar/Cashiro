package com.ritesh.parser.core.bank

class JanaSmallFinanceBankParser : BaseIndianBankParser() {

    override fun getBankName() = "Jana Small Finance Bank"

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("JANABK") ||
                normalizedSender.contains("JANASFB")
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val keyword = if (message.contains("credited", ignoreCase = true)) "from" else "to"
        val pattern = Regex(
            """\b$keyword\s+(.+?)(?:\.\s|\s+UPI\b|$)""",
            RegexOption.IGNORE_CASE
        )
        pattern.find(message)?.let { match ->
            var name = match.groupValues[1].trim()
            if (name.contains("@")) name = name.substringBefore("@")
            name = name.trimEnd('.', ',', ';')
            val merchant = cleanMerchantName(name)
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        return super.extractMerchant(message, sender)
    }
}
