package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class NDBBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        NDBBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "LKR 100.00 debited from AC XXXXXXXX1000 as POS TXN at Example Shop. Avl Bal 900.00", "NDB ALERT", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "LKR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "NDB ALERT", shouldParse = false)
        ), suiteName = "Synthetic NDBBank"
    )
}
