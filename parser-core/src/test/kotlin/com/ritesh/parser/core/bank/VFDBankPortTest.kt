package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class VFDBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        VFDBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "Acct: xxx1000\nAmt: N100.00 DR\nChgs: N1.00\nDesc: Example Shop\nBalance:N900.00", "VFD", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "NGN", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "VFD", shouldParse = false)
        ), suiteName = "Synthetic VFDBank"
    )
}
