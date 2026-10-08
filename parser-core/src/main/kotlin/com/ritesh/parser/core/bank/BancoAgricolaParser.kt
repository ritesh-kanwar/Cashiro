package com.ritesh.parser.core.bank

import com.ritesh.parser.core.ParsedTransaction

class BancoAgricolaParser : BaseElSalvadorBankParser() {

    override fun getBankName() = "Banco Agricola"

    override fun canHandle(sender: String): Boolean {
        val s = sender.uppercase()
        return s.contains("AGRICOLA") || s.contains("TRANSFER365")
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        if (!sender.uppercase().contains("AGRICOLA") && !smsBody.uppercase().contains("AGRICOLA")) {
            return null
        }
        return super.parse(smsBody, sender, timestamp)
    }
}
