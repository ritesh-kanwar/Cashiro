package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class JanaSmallFinanceBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        JanaSmallFinanceBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "Your acct XX1000 is credited with INR 100.00 on 01-Jan-26 from Example Shop. UPI Ref no 000000000001. JANA SFB", "JM-JANABK-S", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.INCOME)),
            ParserTestCase("Unrelated message", "Example service notice", "JM-JANABK-S", shouldParse = false)
        ), suiteName = "Synthetic JanaSmallFinanceBank"
    )
}
