package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class NepalBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        NepalBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "Your account ####1000 debited NPR 100.00 on 01-01-2026.", "NBL_ALERT", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "NPR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "NBL_ALERT", shouldParse = false)
        ), suiteName = "Synthetic NepalBank"
    )
}
