package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class BancoCuscatlanPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        BancoCuscatlanParser(), listOf(
            ParserTestCase("Synthetic transaction", "Compra CUSCATLAN 1000 por USD100.00 en Example Shop el 01/01/2026.", "BCUSCATLAN", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "USD", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "BCUSCATLAN", shouldParse = false)
        ), suiteName = "Synthetic BancoCuscatlan"
    )
}
