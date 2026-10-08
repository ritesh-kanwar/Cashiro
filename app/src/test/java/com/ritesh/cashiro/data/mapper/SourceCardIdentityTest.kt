package com.ritesh.cashiro.data.mapper

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class SourceCardIdentityTest {
    private fun source() = ParsedTransaction(BigDecimal("100"), TransactionType.EXPENSE, "Example Shop", null,
        "1000", null, smsBody = "Synthetic alert", sender = "EXAMPLE-T", timestamp = 0, bankName = "Example Bank")
    @Test fun eitherParserCardFlagOrOriginalCreditTypeIdentifiesCard() {
        assertTrue(source().copy(isFromCard = true).isSourceCard)
        assertTrue(source().copy(type = TransactionType.CREDIT).isSourceCard)
    }
    @Test fun ordinaryBankSourceRemainsSeparateFromRuleSelectedCredit() {
        assertFalse(source().isSourceCard)
    }
}
