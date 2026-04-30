package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ManjushreeFinanceParserTest {
    @Test
    fun `manjushree parser handles debited example`() {
        val parser = ManjushreeFinanceParser()

        ParserTestUtils.printTestHeader(
            parserName = "Manjushree Finance",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val cases = listOf(
            ParserTestCase(
                name = "Debited example",
                message = """Your A/C ##0168658000001, has been debited by NPR 15,000.00 on 01/04/2026 10:27,Remarks:9769780059~2309320,IBFT,transfer laxmi paudyal~RBB
Manjushree Finance""",
                sender = "MFL_ALERT",
                expected = ExpectedTransaction(
                    amount = BigDecimal("15000.00"),
                    currency = "NPR",
                    type = TransactionType.EXPENSE,
                    reference = "9769780059~2309320",
                    merchant = "laxmi paudyal"
                )
            )
        )

        val handleCases = listOf(
            "MFL_ALERT" to true,
            "MFL" to true,
            "MANJUSHREE" to true,
            "UNKNOWN" to false
        )

        ParserTestUtils.runTestSuite(parser, cases, handleCases, "Manjushree Finance Parser Tests")
    }
}
