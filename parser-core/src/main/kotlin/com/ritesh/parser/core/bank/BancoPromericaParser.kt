package com.ritesh.parser.core.bank

class BancoPromericaParser : BaseElSalvadorBankParser() {

    override fun getBankName() = "Banco Promerica"

    override fun canHandle(sender: String) = sender.uppercase().contains("PROMERICA")

    override val accountPatterns = listOf(
        Regex("""TTA\s*\*?(\d{4})""", RegexOption.IGNORE_CASE)
    )
}
