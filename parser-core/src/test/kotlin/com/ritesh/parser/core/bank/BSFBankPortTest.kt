package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class BSFBankPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        BSFBankParser(), listOf(
            ParserTestCase("Synthetic transaction", "عملية حوالة مالية صادرة مقبولة\nخصمت من حساب ****1000\nإلى Example Shop\nالقيمة SAR 100.00\nالرسوم SAR 1.00", "BSF", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "SAR", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "BSF", shouldParse = false)
        ), suiteName = "Synthetic BSFBank"
    )
}
