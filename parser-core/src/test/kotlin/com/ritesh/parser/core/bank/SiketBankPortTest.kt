package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class SiketBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        SiketBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "Your Account ****1000 Credited with ETB 100.00. Current Balance is ETB 900.00. Reference number 000000000001", "SIKET BANK", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "ETB", type = TransactionType.INCOME)),
            ParserTestCase("Unrelated message", "Example service notice", "SIKET BANK", shouldParse = false)
        ), suiteName = "Synthetic SiketBank"
    )
}
