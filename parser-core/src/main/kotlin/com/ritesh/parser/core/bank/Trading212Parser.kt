package com.ritesh.parser.core.bank

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType

class Trading212Parser : BankParser() {

    override fun getBankName() = "Trading 212"

    override fun getCurrency() = "GBP"

    override fun canHandle(sender: String): Boolean =
        sender.uppercase().replace(Regex("""[\s_-]"""), "") == "TRADING212"

    private val interestPattern = Regex(
        """earned\s+£\s*([0-9,]+(?:\.\d{1,2})?)\s+interest""",
        RegexOption.IGNORE_CASE
    )

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        val m = interestPattern.find(smsBody) ?: return null
        val amount = m.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
        return ParsedTransaction(
            amount = amount,
            type = TransactionType.INCOME,
            merchant = "Trading 212 Interest",
            reference = null,
            accountLast4 = null,
            balance = null,
            smsBody = smsBody,
            sender = sender,
            timestamp = timestamp,
            bankName = getBankName(),
            currency = getCurrency()
        )
    }
}
