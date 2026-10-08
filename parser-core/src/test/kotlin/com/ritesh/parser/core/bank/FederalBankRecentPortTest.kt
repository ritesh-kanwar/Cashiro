package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class FederalBankRecentPortTest {
    @TestFactory fun recentFixes() = ParserTestUtils.runTestSuite(FederalBankParser(), listOf(
        ParserTestCase("New FEDSMS sender", "Rs.100.00 debited via UPI from your A/c XX1000 to VPA example@bank Ref 000000000001", "AX-FEDSMS-S", expected = ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.EXPENSE, accountLast4 = "1000")),
        ParserTestCase("Scapia rewards transaction", "Your txn of ₹100.00 at Example Shop on your Scapia Federal credit card ending with 1000 earned you rewards.", "FEDSCP", expected = ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.CREDIT, merchant = "Example Shop", accountLast4 = "1000", expectNoReference = true)),
        ParserTestCase("Scapia failed transaction", "Your txn of ₹100.00 at Example Shop on your Scapia Federal credit card was declined.", "FEDSCP", shouldParse = false)
    ), suiteName = "Synthetic recent fixes")
}
