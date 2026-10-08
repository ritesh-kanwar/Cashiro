package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class NationsTrustBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        NationsTrustBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "Transaction for LKR 100.00 approved on your Card 0****1000 at Example Shop Available Bal LKR 900.00", "NationsSMS", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "LKR", type = TransactionType.CREDIT)),
            ParserTestCase("Unrelated message", "Example service notice", "NationsSMS", shouldParse = false)
        ), suiteName = "Synthetic NationsTrustBank"
    )
}
