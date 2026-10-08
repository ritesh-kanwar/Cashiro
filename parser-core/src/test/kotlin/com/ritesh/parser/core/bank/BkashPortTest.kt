package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class BkashPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        BkashParser(), listOf(
            ParserTestCase("Synthetic transaction", "Payment of Tk 100.00 is successful. Balance Tk 900.00. TrxID 000000000001", "bKash", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "BDT", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "bKash", shouldParse = false)
        ), suiteName = "Synthetic Bkash"
    )
}
