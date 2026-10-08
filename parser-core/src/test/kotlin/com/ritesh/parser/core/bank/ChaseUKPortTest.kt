package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class ChaseUKPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        ChaseUKParser(), listOf(
            ParserTestCase("Synthetic transaction", "£100.00 just landed in Example Account from Example Shop", "ChaseUK", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "GBP", type = TransactionType.INCOME)),
            ParserTestCase("Unrelated message", "Example service notice", "ChaseUK", shouldParse = false)
        ), suiteName = "Synthetic ChaseUK"
    )
}
