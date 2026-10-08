package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class CitizensBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        CitizensBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "Your account ####1000 is debited NPR 100.00. Remarks: Example Shop Av Bal: 900.00", "CTZN_ALERT", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "NPR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "CTZN_ALERT", shouldParse = false)
        ), suiteName = "Synthetic CitizensBank"
    )
}
