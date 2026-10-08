package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class HDFCBankRecentPortTest {
    @TestFactory fun recentFixes() = ParserTestUtils.runTestSuite(HDFCBankParser(), listOf(
        ParserTestCase("Interest paid narration remains income", "INR 100.00 deposited in HDFC Bank A/c XX1000 for Interest paid till 01-01-2026", "HDFCBK", expected = ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.INCOME, accountLast4 = "1000")),
        ParserTestCase("PIXEL compact card block instruction", "Rs.100.00 used on HDFC Bank PIXEL Card at Example Shop on 01-01-2026. SMS BLOCKPCC 1000", "HDFCBK", expected = ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.CREDIT, accountLast4 = "1000")),
        ParserTestCase("Bill reminder is not an expense", "Your bill of INR 100.00 due on 01-01-2026. Pay now.", "HDFCBK", shouldParse = false),
        ParserTestCase("Completed transfer mentioning due date remains expense", "Sent Rs.100.00 From HDFC Bank A/C *1000 To Example Shop On 01/01/26 for EMI due on 02-01-2026", "HDFCBK", expected = ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.EXPENSE, merchant = "Example Shop", accountLast4 = "1000"))
    ), suiteName = "Synthetic recent fixes")
}
