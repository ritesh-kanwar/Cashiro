package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class LumbiniBikashBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        LumbiniBikashBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "NPR 100.00 withdrawn from A/C ####1000. Remarks: Example Shop;", "LBBL_SMART", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "NPR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "LBBL_SMART", shouldParse = false)
        ), suiteName = "Synthetic LumbiniBikashBank"
    )
}
