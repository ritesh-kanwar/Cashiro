package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class Trading212PortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        Trading212Parser(), listOf(
            ParserTestCase("Synthetic transaction", "You earned £100.00 interest on uninvested cash!", "Trading212", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "GBP", type = TransactionType.INCOME)),
            ParserTestCase("Unrelated message", "Example service notice", "Trading212", shouldParse = false)
        ), suiteName = "Synthetic Trading212"
    )
}
