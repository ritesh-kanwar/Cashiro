package com.ritesh.parser.core.bank

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

class NationalBankOfEgyptParser : BankParser() {

    override fun getBankName() = "National Bank of Egypt"

    override fun getCurrency() = "EGP"

    override fun canHandle(sender: String): Boolean =
        sender.uppercase().replace(Regex("""[\s\-_]"""), "").contains("ALAHLY")

    private val cardPattern = Regex(
        """تم\s*خصم\s*([\d,]+(?:\.\d+)?)\s*(?:جم|EGP)\s*من\s*بطاقة\s*(الائتمان|الخصم\s*المباشر)\s*""" +
            """رقم\s*(\d+)\s*عند\s*(.+?)\s*يوم.*?المتاح\s*([\d,]+(?:\.\d+)?)""",
        RegexOption.DOT_MATCHES_ALL
    )

    private val transferPattern = Regex(
        """تم\s*[إا]ضافة\s*تحويل\s*لحظي\s*لحسابكم\s*رقم\s*(\d+)\s*بمبلغ\s*([\d,]+(?:\.\d+)?)\s*""" +
            """(?:جم|EGP)\s*من\s*(.+?)\s*رقم\s*مرجعي\s*(\d+)""",
        RegexOption.DOT_MATCHES_ALL
    )

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        cardPattern.find(smsBody)?.let { m ->
            val isCreditCard = m.groupValues[2].startsWith("الائتمان")
            val available = amount(m.groupValues[5])
            return ParsedTransaction(
                amount = amount(m.groupValues[1]) ?: return null,
                type = TransactionType.EXPENSE,
                merchant = m.groupValues[4].trim(),
                reference = null,
                accountLast4 = m.groupValues[3].takeLast(4),
                balance = if (isCreditCard) null else available,
                creditLimit = if (isCreditCard) available else null,
                smsBody = smsBody,
                sender = sender,
                timestamp = timestamp,
                bankName = getBankName(),
                isFromCard = true,
                currency = getCurrency()
            )
        }

        transferPattern.find(smsBody)?.let { m ->
            return ParsedTransaction(
                amount = amount(m.groupValues[2]) ?: return null,
                type = TransactionType.INCOME,
                merchant = m.groupValues[3].trim(),
                reference = m.groupValues[4],
                accountLast4 = m.groupValues[1].takeLast(4),
                balance = null,
                smsBody = smsBody,
                sender = sender,
                timestamp = timestamp,
                bankName = getBankName(),
                currency = getCurrency()
            )
        }

        return null
    }

    private fun amount(raw: String): BigDecimal? = raw.replace(",", "").toBigDecimalOrNull()
}
