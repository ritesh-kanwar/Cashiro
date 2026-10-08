package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

abstract class BaseElSalvadorBankParser : BankParser() {

    override fun getCurrency() = "USD"

    protected open val accountPatterns: List<Regex> = emptyList()

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        if (lower.contains("codigo") || lower.contains("clave") || lower.contains("otp")) return false
        if (FinancialMessageSafety.isSecurityCode(message)) return false
        if (FinancialMessageSafety.isOperationalOrPromotionalNotice(message)) return false
        if (FinancialMessageSafety.hasExplicitFailure(message, SPANISH_FAILURES)) return false
        if (SPANISH_FAILURES.any { lower.contains(it) }) return false
        return extractTransactionType(message) != null
    }

    override fun extractAmount(message: String): BigDecimal? =
        AMOUNT.find(message)?.groupValues?.get(1)?.replace(",", "")?.toBigDecimalOrNull()

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("recibido") -> TransactionType.INCOME
            DEBIT_MARKERS.any { lower.contains(it) } -> TransactionType.EXPENSE
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        PURCHASE.find(message)?.let { return it.groupValues[1].trim().trimEnd('.', ',') }

        if (extractTransactionType(message) == TransactionType.INCOME) {
            return FROM_PARTY.find(message)?.groupValues?.get(1)?.trim()?.trimEnd('.', ',')
        }
        return TO_PARTY.find(message)?.groupValues?.get(1)?.trim()
    }

    override fun extractAccountLast4(message: String): String? =
        accountPatterns.firstNotNullOfOrNull { it.find(message)?.groupValues?.get(1) }

    override fun detectIsCard(message: String) = CARD.containsMatchIn(message)

    private companion object {
        val AMOUNT = Regex("""(?:USD|US\$|\$)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val DEBIT_MARKERS = listOf("debito", "consumo", "compra", "retiro", "aplicada", "pago")
        val PURCHASE = Regex(
            """por\s+(?:USD|US\$|\$)\s*[\d,.]+\s+en\s+(.+?)(?:\s+el\s+\d|\.\s|\.?$)""",
            RegexOption.IGNORE_CASE
        )
        val FROM_PARTY = Regex("""\bdesde\s+(.+?)(?:\s+el\s+\d|\.\s|\.?$)""", RegexOption.IGNORE_CASE)
        val SPANISH_FAILURES = listOf(
            "rechazada", "rechazado", "denegada", "denegado", "no aplicada", "no aplicado",
            "fallida", "fallido", "no procesada", "no procesado", "sin exito", "no exitosa",
            "solicitud de pago", "solicita un pago", "intento de"
        )
        val TO_PARTY = Regex(""".*\ba\s+(.+?)\s+por\s+(?:USD|US\$|\$)""", RegexOption.IGNORE_CASE)
        val CARD = Regex("""tarjeta|\bTTA\b""", RegexOption.IGNORE_CASE)
    }
}
