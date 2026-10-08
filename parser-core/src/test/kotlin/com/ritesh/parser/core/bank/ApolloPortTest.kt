package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class ApolloPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        ApolloParser(), listOf(
            ParserTestCase("Synthetic transaction", "Your account 1*00 was credited with ETB 100.00 by Example Shop. Available Balance: ETB 900.00.", "apollo", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "ETB", type = TransactionType.INCOME, isMobileWallet = true)),
            ParserTestCase("Unrelated message", "Example service notice", "apollo", shouldParse = false)
        ), suiteName = "Synthetic Apollo"
    )
}
