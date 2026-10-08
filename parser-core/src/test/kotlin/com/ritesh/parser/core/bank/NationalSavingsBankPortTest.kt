package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class NationalSavingsBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        NationalSavingsBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "LKR 100.00 Debited from your A/c XXXXXXXX1000 on 01/01/2026 at 12:00. AvlBal LKR 900.00. @ Example Shop. ATM POS Transaction.", "NSB", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "LKR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "NSB", shouldParse = false)
        ), suiteName = "Synthetic NationalSavingsBank"
    )
}
