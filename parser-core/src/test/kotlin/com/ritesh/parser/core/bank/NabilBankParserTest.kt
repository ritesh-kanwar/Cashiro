package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class NabilBankParserTest {
    @Test
    fun `nabil parser handles withdrawn example`() {
        val parser = NabilBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "Nabil Bank",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val cases = listOf(
            ParserTestCase(
                name = "Withdrawn example",
                message = """Dear Customer, Your 091##04118 has been withdrawn by NPR 20,008.00 on 17/04/2026 07:58:06, Remarks: MTXN0000517374-130
Download App: https://rebrand.ly/nBank""",
                sender = "NABIL_ALERT",
                expected = ExpectedTransaction(
                    amount = BigDecimal("20008.00"),
                    currency = "NPR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "4118",
                    reference = "MTXN0000517374-130"
                )
            )
        )

        val handleCases = listOf(
            "NABIL_ALERT" to true,
            "NABIL" to true,
            "NMB_ALERT" to false
        )

        ParserTestUtils.runTestSuite(parser, cases, handleCases, "Nabil Bank Parser Tests")
    }

    @Test
    fun `nabil parser only extracts account last4 from account-like context`() {
        val parser = NabilBankParser()

        val accountLikeMessage = parser.parse(
            "Dear Customer, Your 091##04118 has been withdrawn by NPR 20,008.00 on 17/04/2026 07:58:06, Remarks: MTXN0000517374-130",
            "NABIL_ALERT",
            System.currentTimeMillis()
        )

        assertNotNull(accountLikeMessage)
        assertEquals("4118", accountLikeMessage?.accountLast4)

        assertNull(
            parser.parse(
                "Dear Customer, NPR 20,008.00 was debited on 17/04/2026 07:58:06, Remarks: MTXN0000517374-130",
                "NABIL_ALERT",
                System.currentTimeMillis()
            )?.accountLast4
        )
    }
}
