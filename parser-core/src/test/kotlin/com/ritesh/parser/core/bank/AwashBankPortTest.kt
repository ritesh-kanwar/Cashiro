package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class AwashBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        AwashBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "ETB 100.00 has been credited to your account from Example Shop on: 01-01-2026. Balance is ETB 900.00.", "AWASH BANK", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "ETB", type = TransactionType.INCOME)),
            ParserTestCase("Unrelated message", "Example service notice", "AWASH BANK", shouldParse = false)
        ), suiteName = "Synthetic AwashBank"
    )
}
