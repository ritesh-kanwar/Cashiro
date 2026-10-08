package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class ZamZamBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        ZamZamBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "Your account ****1000 has been credited by Example Shop with ETB 100.00. Your current balance is ETB 900.00.", "ZAMZAM BANK", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "ETB", type = TransactionType.INCOME)),
            ParserTestCase("Unrelated message", "Example service notice", "ZAMZAM BANK", shouldParse = false)
        ), suiteName = "Synthetic ZamZamBank"
    )
}
