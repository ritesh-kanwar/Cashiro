package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class NationalBankOfEgyptPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        NationalBankOfEgyptParser(), listOf(
            ParserTestCase("Synthetic transaction", "تم إضافة تحويل لحظي لحسابكم رقم 1000 بمبلغ 100.00 جم من Example Shop رقم مرجعي 000000000001", "BanK-AlAhly", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "EGP", type = TransactionType.INCOME)),
            ParserTestCase("Unrelated message", "Example service notice", "BanK-AlAhly", shouldParse = false)
        ), suiteName = "Synthetic NationalBankOfEgypt"
    )
}
