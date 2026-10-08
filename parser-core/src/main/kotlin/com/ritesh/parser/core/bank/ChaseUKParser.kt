package com.ritesh.parser.core.bank

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class ChaseUKParser : BankParser() {

    override fun getBankName() = "Chase UK"

    override fun getCurrency() = "GBP"

    override fun canHandle(sender: String): Boolean =
        sender.uppercase().replace(Regex("""[\s_-]"""), "") == "CHASEUK"

    private val moneyInPattern = Regex(
        """£\s*([0-9,]+(?:\.\d{1,2})?)\s+just\s+landed\s+in\s+.+?\s+from\s+(.+?)\s*[.!]?\s*$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        val m = moneyInPattern.find(smsBody) ?: return null
        val amount = m.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
        return ParsedTransaction(
            amount = amount,
            type = TransactionType.INCOME,
            merchant = m.groupValues[2].trim().ifEmpty { null },
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
