package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class PluxeeBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        PluxeeBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "Rs.100.00 spent on card no. XX1000 at Example Shop. Avl bal Rs.900.00", "PLUXEE", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "PLUXEE", shouldParse = false)
        ), suiteName = "Synthetic PluxeeBank"
    )
}
