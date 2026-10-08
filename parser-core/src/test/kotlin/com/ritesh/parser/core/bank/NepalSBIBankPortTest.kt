package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class NepalSBIBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        NepalSBIBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "Account XX1000 debited by NPR 100.00. Ref: Example Shop.", "NSBI_ALERT", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "NPR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "NSBI_ALERT", shouldParse = false)
        ), suiteName = "Synthetic NepalSBIBank"
    )
}
