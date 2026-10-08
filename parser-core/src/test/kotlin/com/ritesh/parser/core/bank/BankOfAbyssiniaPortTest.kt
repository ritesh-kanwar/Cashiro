package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class BankOfAbyssiniaPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        BankOfAbyssiniaParser(), listOf(
            ParserTestCase("Synthetic transaction", "Your account ****1000 was debited with ETB 100.00. Available Balance: ETB 900.00.", "BOA", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "ETB", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "BOA", shouldParse = false)
        ), suiteName = "Synthetic BankOfAbyssinia"
    )
}
