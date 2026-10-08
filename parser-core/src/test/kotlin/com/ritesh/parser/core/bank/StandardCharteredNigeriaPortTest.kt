package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class StandardCharteredNigeriaPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        StandardCharteredNigeriaParser(), listOf(
            ParserTestCase("Synthetic transaction", "Debit Alert! Acct: xxxxxx1000, Amt: NGN100.00, Desc: Example Shop, Date: 01-Jan-2026, Bal: NGN900.00", "SCBANK", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "NGN", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "SCBANK", shouldParse = false)
        ), suiteName = "Synthetic StandardCharteredNigeria"
    )
}
