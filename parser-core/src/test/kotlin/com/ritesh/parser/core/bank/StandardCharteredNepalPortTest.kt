package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class StandardCharteredNepalPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        StandardCharteredNepalParser(), listOf(
            ParserTestCase("Synthetic transaction", "NPR 100.00 debited from your account 00001000 on 01-01-2026.", "SC_ALERT", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "NPR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "SC_ALERT", shouldParse = false)
        ), suiteName = "Synthetic StandardCharteredNepal"
    )
}
