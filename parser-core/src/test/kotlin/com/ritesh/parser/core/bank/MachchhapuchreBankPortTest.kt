package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class MachchhapuchreBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        MachchhapuchreBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "NPR 100.00 withdrawn from account ####1000. Remarks: Example Shop Available Bal: 900.00", "MBL_ALERT", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "NPR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "MBL_ALERT", shouldParse = false)
        ), suiteName = "Synthetic MachchhapuchreBank"
    )
}
