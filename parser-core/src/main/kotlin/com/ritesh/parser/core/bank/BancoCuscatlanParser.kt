package com.ritesh.parser.core.bank

class BancoCuscatlanParser : BaseElSalvadorBankParser() {

    override fun getBankName() = "Banco Cuscatlan"

    override fun canHandle(sender: String): Boolean {
        val s = sender.uppercase().replace(".", "").replace(" ", "")
        return s == "BCUSCATLAN" || s == "CUSCATLAN" || s == "BANCOCUSCATLAN"
    }

    override val accountPatterns = listOf(
        Regex("""CUSCATLAN\s+(\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""(?:CORRIENTE|AHORROS?)\s+X*(\d{4})""", RegexOption.IGNORE_CASE)
    )
}
