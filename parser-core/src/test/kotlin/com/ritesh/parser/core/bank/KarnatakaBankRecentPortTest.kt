package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class KarnatakaBankRecentPortTest {
    @TestFactory fun recentFixes() = ParserTestUtils.runTestSuite(KarnatakaBankParser(), listOf(
        ParserTestCase("Payee initials retained and masked UPI reference ignored", "Your a/c XX1000 debited for Rs.100.00 on 01-01-26 trf to E. X. Example Shop. UPI:0*********01.For dispute contact bank.", "KARBANK", expected = ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.EXPENSE, merchant = "E. X. Example Shop", accountLast4 = "1000", expectNoReference = true))
    ), suiteName = "Synthetic recent fixes")
}
