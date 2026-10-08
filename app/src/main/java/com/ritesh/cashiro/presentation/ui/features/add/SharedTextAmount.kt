package com.ritesh.cashiro.presentation.ui.features.add

import java.math.BigDecimal
import java.util.Currency

/** Conservative local draft extraction. Ambiguous figures remain for the user to enter. */
object SharedTextAmount {
    data class Guess(val amount: BigDecimal, val currency: String?)
    private const val END = """(?![\w,]|\.\d)"""
    private const val NUMBER = """(\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?|\d{1,2}(?:,\d{2})*,\d{3}(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)"""
    private val codes = Currency.getAvailableCurrencies().map { it.currencyCode }.sorted().joinToString("|")
    private val tagged = Regex("""([₹$€£৳₦]|\b(?:rs\.?|$codes)(?![a-z]))\s*$NUMBER$END""", RegexOption.IGNORE_CASE)
    private val currencyTag = Regex("""[₹$€£৳₦]|\b(?:rs\.?|$codes)\b""", RegexOption.IGNORE_CASE)
    private val bare = Regex("""(?<![\w/.,-])(\d{1,6}(?:\.\d{1,2})?)$END(?![/-])""")
    private val identifier = Regex("""\b(?:ref(?:erence)?|account|acct|a/c|otp|pin|id|date|balance)\b""", RegexOption.IGNORE_CASE)

    fun extract(text: String): Guess? {
        val matches = tagged.findAll(text).toList()
        if (matches.size == 1) {
            val match = matches.single()
            val amount = positive(match.groupValues[2]) ?: return null
            val currency = when (val tag = match.groupValues[1].uppercase().trimEnd('.')) {
                "₹", "RS" -> "INR"
                "$" -> "USD"
                "€" -> "EUR"
                "£" -> "GBP"
                "৳" -> "BDT"
                "₦" -> "NGN"
                else -> tag
            }
            return Guess(amount, currency)
        }
        if (matches.isNotEmpty() || currencyTag.containsMatchIn(text) || text.length > 40 || identifier.containsMatchIn(text)) return null
        val numbers = bare.findAll(text).toList()
        if (numbers.size != 1) return null
        return positive(numbers.single().groupValues[1])?.let { Guess(it, null) }
    }

    fun amountForCurrency(text: String, currency: String): BigDecimal? =
        extract(text)?.takeIf { it.currency == null || it.currency == currency }?.amount

    private fun positive(raw: String): BigDecimal? = raw.replace(",", "").takeIf { it.length <= 15 }
        ?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
}
