package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class GTBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        GTBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "Acct: ****1000\nAmt: NGN100.00 DR\nDesc: Example Shop\nBal: NGN900.00", "GTBank", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "NGN", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "GTBank", shouldParse = false)
        ), suiteName = "Synthetic GTBank"
    )
}
