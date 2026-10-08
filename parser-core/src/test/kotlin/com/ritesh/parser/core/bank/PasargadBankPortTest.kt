package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class PasargadBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        PasargadBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "000.1000 -100 01/01_12:00 مانده: 900", "WEPOD", ExpectedTransaction(amount = BigDecimal("100"), currency = "IRR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "WEPOD", shouldParse = false)
        ), suiteName = "Synthetic PasargadBank"
    )
}
