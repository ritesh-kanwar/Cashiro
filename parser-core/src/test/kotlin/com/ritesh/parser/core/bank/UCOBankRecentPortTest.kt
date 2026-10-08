package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class UCOBankRecentPortTest {
    @TestFactory fun recentFixes() = ParserTestUtils.runTestSuite(UCOBankParser(), listOf(
        ParserTestCase("Sub rupee debit cannot become balance", "Your A/c XX1000 Debited with Rs..50 on 01-01-2026. Avl Bal in your A/c is Rs.900.00", "UCOBNK", expected = ExpectedTransaction(amount = BigDecimal("0.50"), currency = "INR", type = TransactionType.EXPENSE, accountLast4 = "1000", balance = BigDecimal("900.00"))),
        ParserTestCase("Unavailable balance is not a charge", "Your A/c XX1000 Debited with Rs.100.00. Avl Bal unavailable. charge Rs.1.00", "UCOBNK", expected = ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.EXPENSE, accountLast4 = "1000", expectNoBalance = true, isMobileWallet = false))
    ), suiteName = "Synthetic recent fixes")
}
