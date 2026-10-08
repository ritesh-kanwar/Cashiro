package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class D360BankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        D360BankParser(), listOf(
            ParserTestCase("Synthetic transaction", "International Online Purchase\nCard: *1000 - VISA\nAmount: TRY 200.00 (SAR 100.00)\nAt: Example Shop", "D360Bank", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "SAR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "D360Bank", shouldParse = false)
        ), suiteName = "Synthetic D360Bank"
    )
}
